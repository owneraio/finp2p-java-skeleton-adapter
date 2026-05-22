package io.ownera.ledger.adapter.vanilla;

import io.ownera.ledger.adapter.service.BusinessException;
import io.ownera.ledger.adapter.service.model.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link VanillaDistributionService}: each {@code DistributionService}
 * method exercised against the real {@link LedgerStorage}, with the on-chain balance source
 * stubbed via Mockito.
 */
class VanillaDistributionServiceTest {

    private LedgerStorage storage;
    private OmnibusDelegate omnibusDelegate;
    private VanillaDistributionService service;

    @BeforeEach
    void setup() {
        storage = new LedgerStorage(VanillaPostgresHolder.CTX, VanillaPostgresHolder.SCHEMA);
        omnibusDelegate = Mockito.mock(OmnibusDelegate.class);
        service = new VanillaDistributionService(storage, omnibusDelegate);
    }

    @Test
    void constructorRejectsNullOmnibusDelegate() {
        // Delegate is the source of truth for on-chain balance; constructing without one is a
        // wiring mistake, so fail fast.
        assertThrows(IllegalArgumentException.class,
                () -> new VanillaDistributionService(storage, null));
    }

    @Test
    void distributeMovesValueFromOmnibusToInvestor() {
        String assetId = "asset-dist-" + System.nanoTime();
        String inv = "inv-" + System.nanoTime();
        // Seed omnibus with a known on-chain balance so the move has funds to draw from.
        storage.ensureAccount(VanillaDistributionService.OMNIBUS_FIN_ID, assetId);
        storage.credit(VanillaDistributionService.OMNIBUS_FIN_ID, "1000", assetId,
                new LedgerDetails("seed-" + System.nanoTime(), null, "issue", null, null));

        service.distribute(inv, assetId, AssetType.FINP2P, "300");

        assertEquals("700", storage.getBalance(VanillaDistributionService.OMNIBUS_FIN_ID, assetId).balance);
        assertEquals("300", storage.getBalance(inv, assetId).balance);
    }

    @Test
    void reclaimMovesValueFromInvestorBackToOmnibus() {
        String assetId = "asset-reclaim-" + System.nanoTime();
        String inv = "inv-" + System.nanoTime();
        storage.ensureAccount(VanillaDistributionService.OMNIBUS_FIN_ID, assetId);
        storage.credit(VanillaDistributionService.OMNIBUS_FIN_ID, "500", assetId,
                new LedgerDetails("seed-" + System.nanoTime(), null, "issue", null, null));
        service.distribute(inv, assetId, AssetType.FINP2P, "200");

        service.reclaim(inv, assetId, AssetType.FINP2P, "150");

        assertEquals("50",  storage.getBalance(inv, assetId).balance);
        assertEquals("450", storage.getBalance(VanillaDistributionService.OMNIBUS_FIN_ID, assetId).balance);
    }

    @Test
    void getDistributionStatusReturnsBreakdown() {
        String assetId = "asset-status-" + System.nanoTime();
        String inv1 = "inv-1-" + System.nanoTime();
        String inv2 = "inv-2-" + System.nanoTime();
        storage.ensureAccount(VanillaDistributionService.OMNIBUS_FIN_ID, assetId);
        storage.credit(VanillaDistributionService.OMNIBUS_FIN_ID, "1000", assetId,
                new LedgerDetails("seed-" + System.nanoTime(), null, "issue", null, null));
        service.distribute(inv1, assetId, AssetType.FINP2P, "300");
        service.distribute(inv2, assetId, AssetType.FINP2P, "200");

        DistributionStatus s = service.getDistributionStatus(assetId, AssetType.FINP2P);
        assertEquals(assetId, s.assetId);
        assertEquals(AssetType.FINP2P, s.assetType);
        assertEquals("1000", s.omnibusBalance);
        assertEquals("500",  s.distributedBalance);
        assertEquals("500",  s.availableBalance);
    }

    @Test
    void syncOmnibusReconcilesAgainstDelegate() {
        String assetId = "asset-sync-" + System.nanoTime();
        String inv = "inv-" + System.nanoTime();
        storage.ensureAccount(VanillaDistributionService.OMNIBUS_FIN_ID, assetId);
        storage.credit(VanillaDistributionService.OMNIBUS_FIN_ID, "500", assetId,
                new LedgerDetails("seed-" + System.nanoTime(), null, "issue", null, null));
        service.distribute(inv, assetId, AssetType.FINP2P, "200");
        // Local view: omnibus available = 300, distributed = 200, total = 500.

        Mockito.when(omnibusDelegate.getOmnibusBalance(assetId, AssetType.FINP2P)).thenReturn("700");

        DistributionStatus s = service.syncOmnibus(assetId, AssetType.FINP2P);
        assertEquals("700", s.omnibusBalance);
        assertEquals("200", s.distributedBalance);
        assertEquals("500", s.availableBalance, "on-chain 700 − already-distributed 200");

        assertEquals("500", storage.getBalance(VanillaDistributionService.OMNIBUS_FIN_ID, assetId).balance);
    }

    @Test
    void syncOmnibusThrowsBusinessExceptionWhenOnChainLessThanDistributed() {
        String assetId = "asset-sync-bad-" + System.nanoTime();
        String inv = "inv-" + System.nanoTime();
        storage.ensureAccount(VanillaDistributionService.OMNIBUS_FIN_ID, assetId);
        storage.credit(VanillaDistributionService.OMNIBUS_FIN_ID, "500", assetId,
                new LedgerDetails("seed-" + System.nanoTime(), null, "issue", null, null));
        service.distribute(inv, assetId, AssetType.FINP2P, "400");

        // On-chain reports 200 — less than the 400 already distributed.
        Mockito.when(omnibusDelegate.getOmnibusBalance(assetId, AssetType.FINP2P)).thenReturn("200");

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.syncOmnibus(assetId, AssetType.FINP2P));
        assertTrue(e.getMessage().contains("200"), e.getMessage());
        assertTrue(e.getMessage().toLowerCase().contains("distributed"), e.getMessage());

        // State unchanged after the failed sync.
        assertEquals("100", storage.getBalance(VanillaDistributionService.OMNIBUS_FIN_ID, assetId).balance);
        assertEquals("400", storage.getBalance(inv, assetId).balance);
    }

    @Test
    void flushDistributionsReclaimsAvailableAndLeavesHeldBalancesInPlace() {
        // Reviewer-flagged: flush used to enumerate raw balance and try to reclaim the full
        // amount, which would trip CHECK(held <= balance) on any account with an outstanding
        // hold — and worse, leave the asset partially flushed because earlier loop iterations
        // had already drained other investors. The fix moves only the spendable portion;
        // held value stays put and the flush completes cleanly across every investor.
        String assetId = "asset-flush-held-" + System.nanoTime();
        // Order the finIds so the all-available account flushes BEFORE the partially-held
        // one. Under the old behavior that ordering was exactly the trap: inv1 would be
        // drained, then the loop would throw on inv2 mid-flush.
        String inv1 = "aaa-" + System.nanoTime();
        String inv2 = "bbb-" + System.nanoTime();
        storage.ensureAccount(VanillaDistributionService.OMNIBUS_FIN_ID, assetId);
        storage.credit(VanillaDistributionService.OMNIBUS_FIN_ID, "1000", assetId,
                new LedgerDetails("seed-" + System.nanoTime(), null, "issue", null, null));
        service.distribute(inv1, assetId, AssetType.FINP2P, "300");
        service.distribute(inv2, assetId, AssetType.FINP2P, "400");
        // Tie up part of inv2's balance in an escrow lock.
        storage.lock(inv2, "250", assetId,
                new LedgerDetails("hold-" + System.nanoTime(), "op-x", "hold", null, null));

        DistributionStatus s = service.flushDistributions(assetId, AssetType.FINP2P);

        // inv1 fully drained, inv2 keeps its 250 held value (balance == held); the available
        // 150 was reclaimed.
        assertEquals("0",   storage.getBalance(inv1, assetId).balance);
        assertEquals("250", storage.getBalance(inv2, assetId).balance,
                "held portion must remain on the investor row");
        assertEquals("250", storage.getBalance(inv2, assetId).held);
        // Post-flush status: omnibus rehydrated by 850 (300 + 150), 250 still "distributed".
        assertEquals("1000", s.omnibusBalance);
        assertEquals("250",  s.distributedBalance);
        assertEquals("750",  s.availableBalance);
    }

    @Test
    void flushDistributionsSkipsFullyHeldAccount() {
        String assetId = "asset-flush-allheld-" + System.nanoTime();
        String inv = "inv-" + System.nanoTime();
        storage.ensureAccount(VanillaDistributionService.OMNIBUS_FIN_ID, assetId);
        storage.credit(VanillaDistributionService.OMNIBUS_FIN_ID, "500", assetId,
                new LedgerDetails("seed-" + System.nanoTime(), null, "issue", null, null));
        service.distribute(inv, assetId, AssetType.FINP2P, "200");
        storage.lock(inv, "200", assetId,
                new LedgerDetails("hold-" + System.nanoTime(), "op-y", "hold", null, null));

        DistributionStatus s = service.flushDistributions(assetId, AssetType.FINP2P);

        // Nothing spendable to reclaim — investor row is untouched.
        assertEquals("200", storage.getBalance(inv, assetId).balance);
        assertEquals("200", storage.getBalance(inv, assetId).held);
        assertEquals("300", s.availableBalance, "omnibus still at 300 (no flush moved anything)");
        assertEquals("200", s.distributedBalance);
    }

    @Test
    void flushDistributionsReclaimsAllInvestorBalances() {
        String assetId = "asset-flush-" + System.nanoTime();
        String inv1 = "inv-1-" + System.nanoTime();
        String inv2 = "inv-2-" + System.nanoTime();
        String inv3 = "inv-3-" + System.nanoTime();
        storage.ensureAccount(VanillaDistributionService.OMNIBUS_FIN_ID, assetId);
        storage.credit(VanillaDistributionService.OMNIBUS_FIN_ID, "1000", assetId,
                new LedgerDetails("seed-" + System.nanoTime(), null, "issue", null, null));
        service.distribute(inv1, assetId, AssetType.FINP2P, "300");
        service.distribute(inv2, assetId, AssetType.FINP2P, "200");
        service.distribute(inv3, assetId, AssetType.FINP2P, "100");

        DistributionStatus afterFlush = service.flushDistributions(assetId, AssetType.FINP2P);

        assertEquals("1000", afterFlush.omnibusBalance);
        assertEquals("0",    afterFlush.distributedBalance);
        assertEquals("1000", afterFlush.availableBalance);
        // Each investor row is zeroed.
        assertEquals("0", storage.getBalance(inv1, assetId).balance);
        assertEquals("0", storage.getBalance(inv2, assetId).balance);
        assertEquals("0", storage.getBalance(inv3, assetId).balance);
    }
}
