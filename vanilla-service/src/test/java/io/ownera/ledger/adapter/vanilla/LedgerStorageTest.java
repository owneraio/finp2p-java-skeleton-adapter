package io.ownera.ledger.adapter.vanilla;

import io.ownera.ledger.adapter.service.BusinessException;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link LedgerStorage} against a real Postgres instance via Testcontainers.
 * Covers each balance-mutating operation, idempotency, and CHECK-constraint enforcement.
 */
class LedgerStorageTest {

    private static final DSLContext CTX = VanillaPostgresHolder.CTX;
    private LedgerStorage storage;

    @BeforeEach
    void setup() {
        storage = new LedgerStorage(CTX, VanillaPostgresHolder.SCHEMA);
    }

    private LedgerDetails details(String key, String operationType) {
        return new LedgerDetails(key, null, operationType, null, null);
    }

    @Test
    void rejectsInvalidSchemaName() {
        assertThrows(IllegalArgumentException.class, () -> new LedgerStorage(CTX, "bad name"));
        assertThrows(IllegalArgumentException.class, () -> new LedgerStorage(CTX, "drop;table"));
        assertThrows(IllegalArgumentException.class, () -> new LedgerStorage(CTX, ""));
    }

    @Test
    void getBalanceReturnsZeroForUnknownAccount() {
        LedgerBalance b = storage.getBalance("unknown-" + System.nanoTime(), "asset-X");
        assertEquals("0", b.balance);
        assertEquals("0", b.held);
        assertEquals("0", b.available);
    }

    @Test
    void creditIncreasesBalanceAndReturnsTxRow() {
        String finId = "fin-" + System.nanoTime();
        String assetId = "asset-" + System.nanoTime();
        storage.ensureAccount(finId, assetId);

        LedgerTransaction tx = storage.credit(finId, "100", assetId, details("ik-credit-" + System.nanoTime(), "issue"));
        assertEquals(finId, tx.destination);
        assertNull(tx.source);
        assertEquals("100", tx.amount);
        assertEquals("credit", tx.action);

        LedgerBalance b = storage.getBalance(finId, assetId);
        assertEquals("100", b.balance);
        assertEquals("0", b.held);
        assertEquals("100", b.available);
    }

    @Test
    void debitDecreasesBalance() {
        String finId = "fin-" + System.nanoTime();
        String assetId = "asset-" + System.nanoTime();
        storage.ensureAccount(finId, assetId);
        storage.credit(finId, "50", assetId, details("ik-c-" + System.nanoTime(), "issue"));

        storage.debit(finId, "20", assetId, details("ik-d-" + System.nanoTime(), "redeem"));

        assertEquals("30", storage.getBalance(finId, assetId).balance);
    }

    @Test
    void lockMovesAvailableIntoHeld() {
        String finId = "fin-" + System.nanoTime();
        String assetId = "asset-" + System.nanoTime();
        storage.ensureAccount(finId, assetId);
        storage.credit(finId, "100", assetId, details("ik-c-" + System.nanoTime(), "issue"));

        storage.lock(finId, "40", assetId, details("ik-l-" + System.nanoTime(), "hold"));

        LedgerBalance b = storage.getBalance(finId, assetId);
        assertEquals("100", b.balance);
        assertEquals("40", b.held);
        assertEquals("60", b.available);
    }

    @Test
    void unlockReturnsHeldToAvailable() {
        String finId = "fin-" + System.nanoTime();
        String assetId = "asset-" + System.nanoTime();
        storage.ensureAccount(finId, assetId);
        storage.credit(finId, "100", assetId, details("ik-c-" + System.nanoTime(), "issue"));
        storage.lock(finId, "40", assetId, details("ik-l-" + System.nanoTime(), "hold"));

        storage.unlock(finId, "40", assetId, details("ik-u-" + System.nanoTime(), "rollback"));

        LedgerBalance b = storage.getBalance(finId, assetId);
        assertEquals("0", b.held);
        assertEquals("100", b.available);
    }

    @Test
    void moveTransfersBetweenAccounts() {
        String src = "fin-src-" + System.nanoTime();
        String dst = "fin-dst-" + System.nanoTime();
        String assetId = "asset-" + System.nanoTime();
        storage.ensureAccount(src, assetId);
        storage.ensureAccount(dst, assetId);
        storage.credit(src, "100", assetId, details("ik-c-" + System.nanoTime(), "issue"));

        storage.move(src, dst, "60", assetId, details("ik-m-" + System.nanoTime(), "transfer"));

        assertEquals("40", storage.getBalance(src, assetId).balance);
        assertEquals("60", storage.getBalance(dst, assetId).balance);
    }

    @Test
    void moveRejectsSelfMove() {
        String finId = "fin-self-" + System.nanoTime();
        String assetId = "asset-" + System.nanoTime();
        storage.ensureAccount(finId, assetId);
        BusinessException e = assertThrows(BusinessException.class, () ->
                storage.move(finId, finId, "10", assetId,
                        details("ik-self-" + System.nanoTime(), "transfer")));
        assertTrue(e.getMessage().contains(finId));
    }

    @Test
    void unlockAndMoveReleasesHoldAndTransfers() {
        String src = "fin-src-" + System.nanoTime();
        String dst = "fin-dst-" + System.nanoTime();
        String assetId = "asset-" + System.nanoTime();
        storage.ensureAccount(src, assetId);
        storage.ensureAccount(dst, assetId);
        storage.credit(src, "100", assetId, details("ik-c-" + System.nanoTime(), "issue"));
        storage.lock(src, "70", assetId, details("ik-l-" + System.nanoTime(), "hold"));

        storage.unlockAndMove(src, dst, "70", assetId,
                details("ik-um-" + System.nanoTime(), "release"));

        assertEquals("30", storage.getBalance(src, assetId).balance);
        assertEquals("0", storage.getBalance(src, assetId).held);
        assertEquals("70", storage.getBalance(dst, assetId).balance);
    }

    @Test
    void unlockAndDebitReleasesHoldAndDebits() {
        String finId = "fin-" + System.nanoTime();
        String assetId = "asset-" + System.nanoTime();
        storage.ensureAccount(finId, assetId);
        storage.credit(finId, "100", assetId, details("ik-c-" + System.nanoTime(), "issue"));
        storage.lock(finId, "30", assetId, details("ik-l-" + System.nanoTime(), "hold"));

        storage.unlockAndDebit(finId, "30", assetId,
                details("ik-ud-" + System.nanoTime(), "redeem"));

        LedgerBalance b = storage.getBalance(finId, assetId);
        assertEquals("70", b.balance);
        assertEquals("0", b.held);
    }

    @Test
    void duplicateIdempotencyKeyReturnsOriginalTxWithoutMutating() {
        String finId = "fin-" + System.nanoTime();
        String assetId = "asset-" + System.nanoTime();
        storage.ensureAccount(finId, assetId);
        String ik = "ik-dup-" + System.nanoTime();

        LedgerTransaction first = storage.credit(finId, "50", assetId, details(ik, "issue"));
        LedgerTransaction second = storage.credit(finId, "50", assetId, details(ik, "issue"));

        assertEquals(first.id, second.id, "duplicate idempotency key must return the original tx");
        assertEquals("50", storage.getBalance(finId, assetId).balance,
                "duplicate must NOT credit a second time");
    }

    @Test
    void overdraftViolatesBalanceCheckConstraint() {
        String finId = "fin-" + System.nanoTime();
        String assetId = "asset-" + System.nanoTime();
        storage.ensureAccount(finId, assetId);
        storage.credit(finId, "10", assetId, details("ik-c-" + System.nanoTime(), "issue"));

        BusinessException e = assertThrows(BusinessException.class, () ->
                storage.debit(finId, "20", assetId, details("ik-overdraft-" + System.nanoTime(), "redeem")));
        assertTrue(e.getMessage().toLowerCase().contains("insufficient"), e.getMessage());
        // The original 10 stays put.
        assertEquals("10", storage.getBalance(finId, assetId).balance);
    }

    @Test
    void getTransactionRoundTripsAllFields() {
        String finId = "fin-" + System.nanoTime();
        String assetId = "asset-" + System.nanoTime();
        storage.ensureAccount(finId, assetId);
        String ik = "ik-rt-" + System.nanoTime();
        LedgerDetails d = new LedgerDetails(ik, "op-42", "issue",
                new LedgerDetails.LedgerExecutionContext("plan-1", 7), null);

        LedgerTransaction created = storage.credit(finId, "100", assetId, d);
        LedgerTransaction loaded = storage.getTransaction(created.id);
        assertNotNull(loaded);
        assertEquals(created.id, loaded.id);
        assertEquals("100", loaded.amount);
        assertEquals("credit", loaded.action);
        assertEquals(ik, loaded.details.idempotencyKey);
        assertEquals("op-42", loaded.details.operationId);
        assertEquals("issue", loaded.details.operationType);
        assertNotNull(loaded.details.executionContext);
        assertEquals("plan-1", loaded.details.executionContext.planId);
        assertEquals(7, loaded.details.executionContext.sequence);
    }

    @Test
    void findByOperationIdLooksUpByOperationIdInDetails() {
        String finId = "fin-" + System.nanoTime();
        String assetId = "asset-" + System.nanoTime();
        storage.ensureAccount(finId, assetId);
        String opId = "op-" + System.nanoTime();

        LedgerTransaction created = storage.credit(finId, "5", assetId,
                new LedgerDetails("ik-op-" + System.nanoTime(), opId, "issue", null, null));

        LedgerTransaction found = storage.findByOperationId(opId);
        assertNotNull(found);
        assertEquals(created.id, found.id);
    }

    // ─── Distribution queries ───────────────────────────────────────────────

    private static final String OMNIBUS = "__omnibus__";

    @Test
    void listDistributedAccountsReturnsPositiveBalancesExcludingOmnibus() {
        String assetId = "asset-dist-list-" + System.nanoTime();
        String inv1 = "inv-1-" + System.nanoTime();
        String inv2 = "inv-2-" + System.nanoTime();
        String inv3Zero = "inv-3-" + System.nanoTime();
        storage.ensureAccount(OMNIBUS, assetId);
        storage.ensureAccount(inv1, assetId);
        storage.ensureAccount(inv2, assetId);
        storage.ensureAccount(inv3Zero, assetId);
        storage.credit(OMNIBUS, "1000", assetId, details("ik-seed-" + System.nanoTime(), "issue"));
        storage.move(OMNIBUS, inv1, "100", assetId, details("ik-m1-" + System.nanoTime(), "distribute"));
        storage.move(OMNIBUS, inv2, "250", assetId, details("ik-m2-" + System.nanoTime(), "distribute"));
        // inv3Zero exists but balance == 0 → must NOT appear.

        java.util.List<DistributedAccount> rows = storage.listDistributedAccounts(OMNIBUS, assetId, "finp2p");
        assertEquals(2, rows.size());
        assertTrue(rows.stream().anyMatch(r -> r.finId.equals(inv1) && r.balance.equals("100")));
        assertTrue(rows.stream().anyMatch(r -> r.finId.equals(inv2) && r.balance.equals("250")));
        assertTrue(rows.stream().noneMatch(r -> r.finId.equals(OMNIBUS)),
                "omnibus must be excluded regardless of its balance");
        assertTrue(rows.stream().noneMatch(r -> r.finId.equals(inv3Zero)),
                "zero-balance investor must not appear");
    }

    @Test
    void getDistributionStatusReturnsOmnibusDistributedAvailableBreakdown() {
        String assetId = "asset-dist-status-" + System.nanoTime();
        String inv1 = "inv-1-" + System.nanoTime();
        String inv2 = "inv-2-" + System.nanoTime();
        storage.ensureAccount(OMNIBUS, assetId);
        storage.ensureAccount(inv1, assetId);
        storage.ensureAccount(inv2, assetId);
        storage.credit(OMNIBUS, "1000", assetId, details("ik-seed-" + System.nanoTime(), "issue"));
        storage.move(OMNIBUS, inv1, "150", assetId, details("ik-m1-" + System.nanoTime(), "distribute"));
        storage.move(OMNIBUS, inv2, "200", assetId, details("ik-m2-" + System.nanoTime(), "distribute"));

        DistributionTotals t = storage.getDistributionStatus(OMNIBUS, assetId, "finp2p");
        assertEquals("650", t.available, "omnibus row balance after distributing 350");
        assertEquals("350", t.distributed, "sum of inv1 + inv2");
        assertEquals("1000", t.omnibusBalance, "available + distributed");
    }

    @Test
    void getDistributionStatusReturnsZerosWhenAccountsDoNotExist() {
        DistributionTotals t = storage.getDistributionStatus(OMNIBUS, "no-such-asset-" + System.nanoTime(), "finp2p");
        assertEquals("0", t.omnibusBalance);
        assertEquals("0", t.distributed);
        assertEquals("0", t.available);
    }

    @Test
    void syncOmnibusBalanceReconcilesAvailableAgainstDistributed() {
        String assetId = "asset-sync-" + System.nanoTime();
        String inv1 = "inv-1-" + System.nanoTime();
        storage.ensureAccount(OMNIBUS, assetId);
        storage.ensureAccount(inv1, assetId);
        storage.credit(OMNIBUS, "500", assetId, details("ik-seed-" + System.nanoTime(), "issue"));
        storage.move(OMNIBUS, inv1, "100", assetId, details("ik-m-" + System.nanoTime(), "distribute"));
        // Local view: omnibus = 400 available, inv1 = 100, total = 500.

        // Simulated on-chain balance reads 800 (e.g., a mint outside this adapter).
        DistributionTotals t = storage.syncOmnibusBalance(OMNIBUS, assetId, "800", "finp2p");
        assertEquals("800", t.omnibusBalance, "omnibusBalance echoes the caller's on-chain figure");
        assertEquals("100", t.distributed, "distributed unchanged across sync");
        assertEquals("700", t.available, "available = onChain - distributed");

        // Storage state agrees.
        assertEquals("700", storage.getBalance(OMNIBUS, assetId).balance);
        assertEquals("100", storage.getBalance(inv1, assetId).balance);
    }

    @Test
    void syncOmnibusBalanceLessThanDistributedTripsBalanceCheck() {
        String assetId = "asset-sync-low-" + System.nanoTime();
        String inv1 = "inv-1-" + System.nanoTime();
        storage.ensureAccount(OMNIBUS, assetId);
        storage.ensureAccount(inv1, assetId);
        storage.credit(OMNIBUS, "300", assetId, details("ik-seed-" + System.nanoTime(), "issue"));
        storage.move(OMNIBUS, inv1, "200", assetId, details("ik-m-" + System.nanoTime(), "distribute"));
        // distributed = 200, omnibus available = 100, total = 300.

        // On-chain reports 150 — less than the 200 we've already distributed. The UPDATE would
        // push omnibus balance to -50 and trip CHECK (balance >= 0). Storage surfaces this as a
        // RuntimeException; the service layer translates it into a BusinessException.
        assertThrows(RuntimeException.class, () ->
                storage.syncOmnibusBalance(OMNIBUS, assetId, "150", "finp2p"));

        // State unchanged after the failed sync (the constraint aborted the whole statement).
        assertEquals("100", storage.getBalance(OMNIBUS, assetId).balance);
        assertEquals("200", storage.getBalance(inv1, assetId).balance);
    }

    @Test
    void findByOperationIdReturnsLatestWhenMultipleRowsShareOperationId() throws Exception {
        // hold + release + redeem all stamp the caller's operation_id into details, so the
        // same id legitimately appears on multiple ledger rows. Before this fix, fetchOne()
        // threw TooManyRowsException once more than one row matched. Now the query returns
        // the most-recent row (the terminating event).
        String src = "fin-" + System.nanoTime();
        String dst = "fin-dst-" + System.nanoTime();
        String assetId = "asset-multi-" + System.nanoTime();
        storage.ensureAccount(src, assetId);
        storage.ensureAccount(dst, assetId);
        storage.credit(src, "100", assetId, details("ik-c-" + System.nanoTime(), "issue"));
        String opId = "op-multi-" + System.nanoTime();

        LedgerTransaction holdTx = storage.lock(src, "40", assetId,
                new LedgerDetails("ik-h-" + System.nanoTime(), opId, "hold", null, null));
        // Ensure created_at timestamps are distinguishable; the secondary id DESC tiebreaker
        // covers the same-tick case but we want to exercise the timestamp branch here.
        Thread.sleep(5);
        LedgerTransaction releaseTx = storage.unlockAndMove(src, dst, "40", assetId,
                new LedgerDetails("ik-r-" + System.nanoTime(), opId, "release", null, null));

        LedgerTransaction found = storage.findByOperationId(opId);
        assertNotNull(found);
        assertEquals(releaseTx.id, found.id,
                "duplicate operation_id must resolve to the most-recent event, not throw");
        assertNotEquals(holdTx.id, found.id);
    }
}
