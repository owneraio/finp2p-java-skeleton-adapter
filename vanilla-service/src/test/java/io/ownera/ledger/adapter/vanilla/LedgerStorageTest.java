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
}
