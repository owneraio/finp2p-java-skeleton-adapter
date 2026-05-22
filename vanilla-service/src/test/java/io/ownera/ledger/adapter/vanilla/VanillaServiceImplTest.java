package io.ownera.ledger.adapter.vanilla;

import io.ownera.ledger.adapter.service.mapping.AccountMappingService;
import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.AssetBind;
import io.ownera.ledger.adapter.service.model.AssetType;
import io.ownera.ledger.adapter.service.model.Balance;
import io.ownera.ledger.adapter.service.model.Destination;
import io.ownera.ledger.adapter.service.model.ExecutionContext;
import io.ownera.ledger.adapter.service.model.FailedReceiptStatus;
import io.ownera.ledger.adapter.service.model.FinIdAccount;
import io.ownera.ledger.adapter.service.model.OperationType;
import io.ownera.ledger.adapter.service.model.ReceiptOperation;
import io.ownera.ledger.adapter.service.model.Signature;
import io.ownera.ledger.adapter.service.model.Source;
import io.ownera.ledger.adapter.service.model.SuccessReceiptStatus;
import io.ownera.ledger.adapter.service.model.SuccessfulAssetCreation;
import io.ownera.ledger.adapter.service.model.AssetCreationStatus;
import io.ownera.ledger.adapter.service.model.TokenIdentifier;
import io.ownera.ledger.adapter.service.plan.InboundTransferHook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link VanillaServiceImpl}: each service-interface method exercised
 * against the real {@link LedgerStorage}, with delegate hooks driven by Mockito so failure
 * branches and external-transfer ordering can be observed without a live external system.
 */
class VanillaServiceImplTest {

    private LedgerStorage storage;
    private AccountMappingService mapping;

    @BeforeEach
    void setup() {
        storage = new LedgerStorage(VanillaPostgresHolder.CTX, VanillaPostgresHolder.SCHEMA);
        mapping = Mockito.mock(AccountMappingService.class);
    }

    private VanillaServiceImpl service() {
        return new VanillaServiceImpl(storage, mapping);
    }

    private VanillaServiceImpl service(AssetDelegate ad, TransferDelegate td, EscrowDelegate ed) {
        return new VanillaServiceImpl(storage, mapping, ad, td, ed);
    }

    private static Asset asset(String id) {
        return new Asset(id, AssetType.FINP2P);
    }

    private static Signature sig() {
        return null; // signatures are validated by the controller layer, not vanilla
    }

    private static String uniqueIk(String prefix) {
        return prefix + "-" + System.nanoTime();
    }

    // ─── TokenService ───────────────────────────────────────────────────────

    @Test
    void createAssetWithoutDelegateMintsSyntheticCaip19() {
        AssetCreationStatus s = service().createAsset(uniqueIk("ik"), asset("ast-X"),
                null, null, null, null, null);
        assertTrue(s instanceof SuccessfulAssetCreation);
        SuccessfulAssetCreation success = (SuccessfulAssetCreation) s;
        assertNotNull(success.result.tokenId);
        assertFalse(success.result.tokenId.isEmpty());
        assertEquals("db", success.result.reference.network);
        assertEquals("vanilla", success.result.reference.tokenStandard);
    }

    @Test
    void createAssetWithDelegatePassesThrough() {
        io.ownera.ledger.adapter.service.model.AssetCreationResult expected =
                new io.ownera.ledger.adapter.service.model.AssetCreationResult("EXT-123",
                        new io.ownera.ledger.adapter.service.model.LedgerReference("hedera:testnet", "0x0", "HTS", null));
        AssetDelegate delegate = Mockito.mock(AssetDelegate.class);
        Mockito.when(delegate.createAsset(Mockito.anyString(), Mockito.eq("ast-Y"),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(expected);

        AssetCreationStatus s = service(delegate, null, null).createAsset(
                uniqueIk("ik"), asset("ast-Y"), null, null, null, null, null);
        assertTrue(s instanceof SuccessfulAssetCreation);
        assertEquals("EXT-123", ((SuccessfulAssetCreation) s).result.tokenId);
    }

    @Test
    void createAssetWithBindPreservesTokenId() {
        AssetBind bind = new AssetBind(new TokenIdentifier("tok-bound"));
        AssetCreationStatus s = service().createAsset(uniqueIk("ik"), asset("ast-Z"),
                bind, null, null, null, null);
        assertEquals("tok-bound", ((SuccessfulAssetCreation) s).result.tokenId);
    }

    @Test
    void issueCreditsDestinationAndReturnsSuccessReceipt() {
        String dst = "fin-" + System.nanoTime();
        Asset a = asset("ast-iss-" + System.nanoTime());

        ReceiptOperation op = service().issue(uniqueIk("ik"), a,
                new FinIdAccount(dst), "100", null);

        assertTrue(op instanceof SuccessReceiptStatus);
        SuccessReceiptStatus s = (SuccessReceiptStatus) op;
        assertEquals(OperationType.ISSUE, s.receipt.operationType);
        assertEquals(dst, s.receipt.destination.finId);
        assertEquals("100", storage.getBalance(dst, a.assetId).balance);
    }

    @Test
    void transferLocalMovesBetweenAccounts() {
        String src = "fin-src-" + System.nanoTime();
        String dst = "fin-dst-" + System.nanoTime();
        Asset a = asset("ast-tr-" + System.nanoTime());
        VanillaServiceImpl svc = service();
        svc.issue(uniqueIk("ik-iss"), a, new FinIdAccount(src), "100", null);

        ReceiptOperation op = svc.transfer(uniqueIk("ik-tr"), "nonce",
                new Source(src, new FinIdAccount(src)),
                new Destination(dst, new FinIdAccount(dst)),
                a, "40", sig(), null);

        assertTrue(op instanceof SuccessReceiptStatus);
        assertEquals("60", storage.getBalance(src, a.assetId).balance);
        assertEquals("40", storage.getBalance(dst, a.assetId).balance);
    }

    @Test
    void transferExternalSuccessLocksThenUnlockAndDebits() {
        String src = "fin-src-" + System.nanoTime();
        Asset a = asset("ast-ext-" + System.nanoTime());
        VanillaServiceImpl svcSeed = service();
        svcSeed.issue(uniqueIk("ik-iss"), a, new FinIdAccount(src), "100", null);

        TransferDelegate td = Mockito.mock(TransferDelegate.class);
        Mockito.when(td.outboundTransfer(Mockito.anyString(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.eq("30"), Mockito.any())).thenReturn(DelegateResult.success("EXT-TX"));

        // Destination with an external (LedgerAccount-shaped) account triggers the delegate path.
        Destination external = new Destination("fin-ext", new io.ownera.ledger.adapter.service.model.LedgerAccount("wallet", "0xabc"));

        ReceiptOperation op = service(null, td, null).transfer(uniqueIk("ik-ext"), "nonce",
                new Source(src, new FinIdAccount(src)), external, a, "30", sig(), null);

        assertTrue(op instanceof SuccessReceiptStatus);
        assertEquals("70", storage.getBalance(src, a.assetId).balance);
        assertEquals("0", storage.getBalance(src, a.assetId).held,
                "external success path must unlockAndDebit, leaving no residual hold");
    }

    @Test
    void transferExternalFailureUnlocksLocally() {
        String src = "fin-src-" + System.nanoTime();
        Asset a = asset("ast-extf-" + System.nanoTime());
        service().issue(uniqueIk("ik-iss"), a, new FinIdAccount(src), "100", null);

        TransferDelegate td = Mockito.mock(TransferDelegate.class);
        Mockito.when(td.outboundTransfer(Mockito.anyString(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.anyString(), Mockito.any()))
                .thenReturn(DelegateResult.failure("network down"));

        Destination external = new Destination("fin-ext", new io.ownera.ledger.adapter.service.model.LedgerAccount("wallet", "0xabc"));

        ReceiptOperation op = service(null, td, null).transfer(uniqueIk("ik-extf"), "nonce",
                new Source(src, new FinIdAccount(src)), external, a, "30", sig(), null);

        assertTrue(op instanceof FailedReceiptStatus, "external failure must surface as Failed receipt");
        // Balance restored to available; nothing held.
        assertEquals("100", storage.getBalance(src, a.assetId).balance);
        assertEquals("0", storage.getBalance(src, a.assetId).held);
    }

    @Test
    void transferExternalWithoutDelegateReturnsFailed() {
        Destination external = new Destination("fin-ext", new io.ownera.ledger.adapter.service.model.LedgerAccount("wallet", "0xabc"));
        ReceiptOperation op = service().transfer(uniqueIk("ik"), "nonce",
                new Source("fin-src", new FinIdAccount("fin-src")), external, asset("ast-nd"), "1", sig(), null);
        assertTrue(op instanceof FailedReceiptStatus);
    }

    @Test
    void redeemDebitsSource() {
        String src = "fin-src-" + System.nanoTime();
        Asset a = asset("ast-rd-" + System.nanoTime());
        VanillaServiceImpl svc = service();
        svc.issue(uniqueIk("ik-iss"), a, new FinIdAccount(src), "100", null);

        ReceiptOperation op = svc.redeem(uniqueIk("ik-rd"), "nonce",
                new FinIdAccount(src), a, "25", null, sig(), null);
        assertTrue(op instanceof SuccessReceiptStatus);
        assertEquals("75", storage.getBalance(src, a.assetId).balance);
    }

    @Test
    void getBalanceReturnsAvailable() {
        String finId = "fin-" + System.nanoTime();
        Asset a = asset("ast-bal-" + System.nanoTime());
        VanillaServiceImpl svc = service();
        svc.issue(uniqueIk("ik"), a, new FinIdAccount(finId), "100", null);
        svc.hold(uniqueIk("ik2"), "n", new Source(finId, new FinIdAccount(finId)),
                null, a, "30", sig(), "op-1", null);
        assertEquals("70", svc.getBalance(a, finId));
    }

    @Test
    void balanceReturnsBalanceHeldAndAvailable() {
        String finId = "fin-" + System.nanoTime();
        Asset a = asset("ast-bal2-" + System.nanoTime());
        VanillaServiceImpl svc = service();
        svc.issue(uniqueIk("ik"), a, new FinIdAccount(finId), "100", null);
        svc.hold(uniqueIk("ik2"), "n", new Source(finId, new FinIdAccount(finId)),
                null, a, "30", sig(), "op-1", null);

        Balance b = svc.balance(a, finId);
        assertEquals("100", b.current);
        assertEquals("30", b.held);
        assertEquals("70", b.available);
    }

    // ─── EscrowService ──────────────────────────────────────────────────────

    @Test
    void holdLocksFunds() {
        String src = "fin-src-" + System.nanoTime();
        Asset a = asset("ast-hold-" + System.nanoTime());
        VanillaServiceImpl svc = service();
        svc.issue(uniqueIk("ik-iss"), a, new FinIdAccount(src), "100", null);

        ReceiptOperation op = svc.hold(uniqueIk("ik-h"), "n",
                new Source(src, new FinIdAccount(src)), null, a, "40", sig(), "op-1", null);
        assertTrue(op instanceof SuccessReceiptStatus);
        assertEquals("40", storage.getBalance(src, a.assetId).held);
    }

    @Test
    void holdDelegateFailureUnlocksLocally() {
        String src = "fin-src-" + System.nanoTime();
        Asset a = asset("ast-hold-f-" + System.nanoTime());
        service().issue(uniqueIk("ik-iss"), a, new FinIdAccount(src), "100", null);

        EscrowDelegate ed = Mockito.mock(EscrowDelegate.class);
        Mockito.when(ed.hold(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(DelegateResult.failure("ext-fail"));

        ReceiptOperation op = service(null, null, ed).hold(uniqueIk("ik-h"), "n",
                new Source(src, new FinIdAccount(src)), null, a, "40", sig(), "op-1", null);

        assertTrue(op instanceof FailedReceiptStatus);
        assertEquals("0", storage.getBalance(src, a.assetId).held);
    }

    @Test
    void releaseUnlockAndMovesToDestination() {
        String src = "fin-src-" + System.nanoTime();
        String dst = "fin-dst-" + System.nanoTime();
        Asset a = asset("ast-rel-" + System.nanoTime());
        VanillaServiceImpl svc = service();
        svc.issue(uniqueIk("ik-iss"), a, new FinIdAccount(src), "100", null);
        svc.hold(uniqueIk("ik-h"), "n", new Source(src, new FinIdAccount(src)),
                null, a, "60", sig(), "op-1", null);

        ReceiptOperation op = svc.release(uniqueIk("ik-r"),
                new Source(src, new FinIdAccount(src)),
                new Destination(dst, new FinIdAccount(dst)), a, "60", "op-1", null);

        assertTrue(op instanceof SuccessReceiptStatus);
        assertEquals("40", storage.getBalance(src, a.assetId).balance);
        assertEquals("0", storage.getBalance(src, a.assetId).held);
        assertEquals("60", storage.getBalance(dst, a.assetId).balance);
    }

    @Test
    void rollbackReleasesHold() {
        String src = "fin-src-" + System.nanoTime();
        Asset a = asset("ast-rb-" + System.nanoTime());
        VanillaServiceImpl svc = service();
        svc.issue(uniqueIk("ik-iss"), a, new FinIdAccount(src), "50", null);
        svc.hold(uniqueIk("ik-h"), "n", new Source(src, new FinIdAccount(src)),
                null, a, "20", sig(), "op-rb", null);

        ReceiptOperation op = svc.rollback(uniqueIk("ik-rb"),
                new Source(src, new FinIdAccount(src)), a, "20", "op-rb", null);

        assertTrue(op instanceof SuccessReceiptStatus);
        assertEquals("0", storage.getBalance(src, a.assetId).held);
        assertEquals("50", storage.getBalance(src, a.assetId).balance);
    }

    // ─── CommonService ──────────────────────────────────────────────────────

    @Test
    void getReceiptReturnsBuiltReceipt() {
        String dst = "fin-" + System.nanoTime();
        Asset a = asset("ast-rcpt-" + System.nanoTime());
        VanillaServiceImpl svc = service();
        ReceiptOperation issued = svc.issue(uniqueIk("ik"), a, new FinIdAccount(dst), "10", null);
        String txId = ((SuccessReceiptStatus) issued).receipt.id;

        ReceiptOperation looked = svc.getReceipt(txId);
        assertTrue(looked instanceof SuccessReceiptStatus);
        assertEquals(txId, ((SuccessReceiptStatus) looked).receipt.id);
        assertEquals(OperationType.ISSUE, ((SuccessReceiptStatus) looked).receipt.operationType);
    }

    @Test
    void getReceiptNotFoundReturnsFailed() {
        assertTrue(service().getReceipt("does-not-exist-" + System.nanoTime()) instanceof FailedReceiptStatus);
    }

    @Test
    void getReceiptByOperationIdReturnsLatestEventAfterHoldRelease() throws Exception {
        // Reviewer-flagged: hold and release both stamp the same operation_id into the ledger
        // details JSONB, so by release time there are two rows sharing that id. The fallback
        // lookup by operation_id (when the primary tx-id lookup misses) must return the
        // terminating event — and crucially must not throw on multiple matches.
        String src = "fin-src-" + System.nanoTime();
        String dst = "fin-dst-" + System.nanoTime();
        Asset a = asset("ast-lifecycle-" + System.nanoTime());
        VanillaServiceImpl svc = service();
        svc.issue(uniqueIk("ik-iss"), a, new FinIdAccount(src), "100", null);
        String opId = "op-lifecycle-" + System.nanoTime();
        svc.hold(uniqueIk("ik-h"), "n",
                new Source(src, new FinIdAccount(src)), null, a, "40", sig(), opId, null);
        Thread.sleep(5); // ensure timestamps differ so the ORDER BY is exercised
        ReceiptOperation releaseOp = svc.release(uniqueIk("ik-r"),
                new Source(src, new FinIdAccount(src)),
                new Destination(dst, new FinIdAccount(dst)), a, "40", opId, null);
        String releaseTxId = ((SuccessReceiptStatus) releaseOp).receipt.id;

        // Calling getReceipt with the operationId (not the tx id) must succeed and resolve to
        // the release receipt — the latest event in the lifecycle.
        ReceiptOperation looked = svc.getReceipt(opId);
        assertTrue(looked instanceof SuccessReceiptStatus,
                "lookup by operation_id with multiple matches must not throw, got " + looked);
        assertEquals(releaseTxId, ((SuccessReceiptStatus) looked).receipt.id,
                "fallback by operation_id must return the most-recent (release) event");
    }

    @Test
    void operationStatusThrowsBecauseWorkflowProxyOwnsCidLookups() {
        assertThrows(UnsupportedOperationException.class, () -> service().operationStatus("any"));
    }

    // ─── HealthService ──────────────────────────────────────────────────────

    @Test
    void livenessDoesNotThrowWhenDbReachable() {
        assertDoesNotThrow(() -> service().liveness());
        assertDoesNotThrow(() -> service().readiness());
    }

    // ─── InboundTransferHook ────────────────────────────────────────────────

    @Test
    void onInboundTransferCreditsDestination() {
        String dst = "fin-in-" + System.nanoTime();
        Asset a = asset("ast-in-" + System.nanoTime());
        InboundTransferHook.InboundTransferContext ctx = new InboundTransferHook.InboundTransferContext(
                "plan-1", "fin-src", a, dst, "50", 0,
                InboundTransferHook.InstructionResult.receipt("ext-tx-1"));

        service().onInboundTransfer(uniqueIk("ik"), ctx);

        assertEquals("50", storage.getBalance(dst, a.assetId).balance);
    }

    @Test
    void onInboundTransferSkipsCreditWhenDelegateThrowsVerificationError() {
        String dst = "fin-in-" + System.nanoTime();
        Asset a = asset("ast-in-skip-" + System.nanoTime());
        TransferDelegate td = Mockito.mock(TransferDelegate.class);
        Mockito.doThrow(new InboundTransferVerificationError("not on chain"))
                .when(td).onInboundTransfer(Mockito.anyString(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.anyString(), Mockito.any());

        InboundTransferHook.InboundTransferContext ctx = new InboundTransferHook.InboundTransferContext(
                "plan-1", "fin-src", a, dst, "50", 0,
                InboundTransferHook.InstructionResult.receipt("ext-tx-1"));

        service(null, td, null).onInboundTransfer(uniqueIk("ik"), ctx);

        // Verification failed → no credit.
        assertEquals("0", storage.getBalance(dst, a.assetId).balance);
    }
}
