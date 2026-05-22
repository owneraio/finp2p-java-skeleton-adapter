package io.ownera.ledger.adapter.postgres;

import io.ownera.ledger.adapter.PostgresContainerHolder;
import io.ownera.ledger.adapter.api.model.APILedgerAssetIdentifierTypeCAIP19;
import io.ownera.ledger.adapter.api.model.APIOperationStatusCreateAsset;
import io.ownera.ledger.adapter.service.TokenService;
import io.ownera.ledger.adapter.service.TokenServiceException;
import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.AssetBind;
import io.ownera.ledger.adapter.service.model.AssetCreationResult;
import io.ownera.ledger.adapter.service.model.AssetCreationStatus;
import io.ownera.ledger.adapter.service.model.AssetDenomination;
import io.ownera.ledger.adapter.service.model.AssetType;
import io.ownera.ledger.adapter.service.model.Balance;
import io.ownera.ledger.adapter.service.model.ExecutionContext;
import io.ownera.ledger.adapter.service.model.FinIdAccount;
import io.ownera.ledger.adapter.service.model.LedgerReference;
import io.ownera.ledger.adapter.service.model.OperationStatus;
import io.ownera.ledger.adapter.service.model.PendingAssetCreation;
import io.ownera.ledger.adapter.service.model.ReceiptOperation;
import io.ownera.ledger.adapter.service.model.Signature;
import io.ownera.ledger.adapter.service.model.Source;
import io.ownera.ledger.adapter.service.model.Destination;
import io.ownera.ledger.adapter.service.model.SuccessfulAssetCreation;
import io.ownera.ledger.adapter.service.workflow.CallbackClient;
import io.ownera.ledger.adapter.service.workflow.DbOperationStore;
import io.ownera.ledger.adapter.service.workflow.OperationOutputSerializer;
import io.ownera.ledger.adapter.service.workflow.OperationRecord;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
import io.ownera.ledger.adapter.service.workflow.WorkflowArgsCodec;
import io.ownera.ledger.adapter.service.workflow.WorkflowRecovery;
import io.ownera.ledger.adapter.service.workflow.WorkflowServiceProxy;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the PR 1 durable-workflow path: {@link WorkflowServiceProxy} captures method args,
 * persists pending + inputs, runs the real method in the background, finalizes (persists outputs
 * + sends callback), and {@link WorkflowRecovery} replays IN_PROGRESS rows on startup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class WorkflowServiceProxyTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_CONNECTION_STRING", PostgresContainerHolder.POSTGRES::getJdbcUrl);
        registry.add("DB_USERNAME", PostgresContainerHolder.POSTGRES::getUsername);
        registry.add("DB_PASSWORD", PostgresContainerHolder.POSTGRES::getPassword);
    }

    @Autowired
    private DSLContext dsl;

    private OperationStore store;
    private WorkflowArgsCodec argsCodec;

    @BeforeEach
    void setup() {
        store = new DbOperationStore(dsl, "sample_adapter");
        argsCodec = new WorkflowArgsCodec();
    }

    /**
     * Minimal in-memory {@link TokenService} stub. Only {@code createAsset} is exercised here;
     * the other methods throw to fail-fast if called unexpectedly.
     */
    static class StubTokenService implements TokenService {
        final CountDownLatch released = new CountDownLatch(1);
        volatile boolean hold = false;
        volatile @Nullable RuntimeException failWith = null;
        volatile int createCalls = 0;

        @Override
        public AssetCreationStatus createAsset(String idempotencyKey, Asset asset,
                                               @Nullable AssetBind assetBind, @Nullable Object assetMetadata,
                                               @Nullable String assetName, @Nullable String issuerId,
                                               @Nullable AssetDenomination assetDenomination) {
            createCalls++;
            if (hold) {
                try { released.await(); } catch (InterruptedException ignored) {}
            }
            if (failWith != null) throw failWith;
            return new SuccessfulAssetCreation(new AssetCreationResult(
                    "tok-" + asset.assetId,
                    new LedgerReference("hedera:testnet", "0x0", "HTS", null)));
        }

        @Override
        public ReceiptOperation issue(String idempotencyKey, Asset asset, FinIdAccount to, String amount,
                                      @Nullable ExecutionContext exCtx) { throw new UnsupportedOperationException(); }
        @Override
        public ReceiptOperation transfer(String idempotencyKey, String nonce, Source source, Destination destination,
                                         Asset asset, String quantity, Signature signature,
                                         @Nullable ExecutionContext exCtx) { throw new UnsupportedOperationException(); }
        @Override
        public ReceiptOperation redeem(String idempotencyKey, String nonce, FinIdAccount source, Asset asset,
                                       String quantity, @Nullable String operationId, Signature signature,
                                       @Nullable ExecutionContext exCtx) { throw new UnsupportedOperationException(); }
        @Override public String getBalance(Asset asset, String finId) { return "0"; }
        @Override public Balance balance(Asset asset, String finId) { throw new UnsupportedOperationException(); }
    }

    static class RecordingCallback implements CallbackClient {
        volatile @Nullable String lastCid;
        volatile @Nullable OperationStatus lastResult;
        final CountDownLatch fired = new CountDownLatch(1);

        @Override
        public void sendCallback(String cid, OperationStatus result) {
            lastCid = cid;
            lastResult = result;
            fired.countDown();
        }
    }

    private TokenService wrap(StubTokenService target, @Nullable CallbackClient callback) {
        return WorkflowServiceProxy.wrap(TokenService.class, target, store, callback,
                Executors.newCachedThreadPool(), argsCodec,
                OperationOutputSerializer.defaultSerializer(),
                Set.of("createAsset", "issue", "transfer", "redeem"));
    }

    private Asset asset(String id) {
        return new Asset(id, AssetType.FINP2P);
    }

    @Test
    void proxyReturnsPendingImmediatelyAndPersistsRow() throws Exception {
        StubTokenService stub = new StubTokenService();
        stub.hold = true; // keep work in-flight while we inspect storage
        TokenService proxied = wrap(stub, null);
        try {
            String ik = "ik-pending-" + System.nanoTime();
            AssetCreationStatus result = proxied.createAsset(ik, asset("ast-1"), null, null, null, null, null);

            assertTrue(result instanceof PendingAssetCreation,
                    "proxy must return Pending immediately, got " + result.getClass());
            String cid = ((PendingAssetCreation) result).correlationId;
            assertNotNull(cid);

            String stored = store.findOutputsByCid(cid);
            assertNotNull(stored, "pending payload must be persisted at save time");
            // Postgres JSONB normalizes whitespace; parse before comparing.
            com.fasterxml.jackson.databind.JsonNode parsed = new com.fasterxml.jackson.databind.ObjectMapper().readTree(stored);
            assertEquals(false, parsed.at("/operation/isCompleted").asBoolean(true), stored);
        } finally {
            stub.released.countDown();
        }
    }

    @Test
    void proxyFinalizesSuccessAndCallsCallback() throws Exception {
        StubTokenService stub = new StubTokenService();
        RecordingCallback cb = new RecordingCallback();
        TokenService proxied = wrap(stub, cb);

        String ik = "ik-success-" + System.nanoTime();
        AssetCreationStatus result = proxied.createAsset(ik, asset("ast-S"), null, null, null, null, null);
        String cid = ((PendingAssetCreation) result).correlationId;

        assertTrue(cb.fired.await(5, TimeUnit.SECONDS), "callback must fire after persistence");
        assertEquals(cid, cb.lastCid);
        assertTrue(cb.lastResult instanceof SuccessfulAssetCreation, "callback must carry the success payload");

        OperationRecord rec = store.findByCid(cid);
        assertNotNull(rec);
        assertEquals(OperationRecord.Status.COMPLETED, rec.status);
        assertTrue(rec.result instanceof SuccessfulAssetCreation,
                "result must be reconstructable from persisted outputs");
    }

    @Test
    void proxyFinalizesFailureAndCallsCallback() throws Exception {
        StubTokenService stub = new StubTokenService();
        stub.failWith = new RuntimeException("simulated failure");
        RecordingCallback cb = new RecordingCallback();
        TokenService proxied = wrap(stub, cb);

        String ik = "ik-failure-" + System.nanoTime();
        AssetCreationStatus result = proxied.createAsset(ik, asset("ast-F"), null, null, null, null, null);
        String cid = ((PendingAssetCreation) result).correlationId;

        assertTrue(cb.fired.await(5, TimeUnit.SECONDS), "callback must also fire on failure");
        assertEquals(cid, cb.lastCid);
        assertNotNull(cb.lastResult, "callback must carry the failure payload");

        OperationRecord rec = store.findByCid(cid);
        assertNotNull(rec);
        assertEquals(OperationRecord.Status.FAILED, rec.status);
    }

    @Test
    void tryInsertOnInputsHashConflictReturnsFalseInsteadOfThrowing() {
        // Direct store-level race proof: two tryInsert calls with the same inputs_hash and
        // different cids must NOT both succeed and must NOT throw on the second. The second
        // call gets false and the row keeps the first cid.
        String inputsHash = "hash-race-" + System.nanoTime();
        OperationRecord first = new OperationRecord(
                "cid-first-" + System.nanoTime(), "createAsset",
                OperationRecord.Status.IN_PROGRESS, inputsHash, null);
        OperationRecord second = new OperationRecord(
                "cid-second-" + System.nanoTime(), "createAsset",
                OperationRecord.Status.IN_PROGRESS, inputsHash, null);

        assertTrue(store.tryInsert(first, (String) null, "{\"a\":1}"), "first insert must win");
        assertFalse(store.tryInsert(second, (String) null, "{\"a\":2}"),
                "second insert with the same inputs_hash must report conflict (false), not throw");

        OperationRecord winner = store.findByInputsHash(inputsHash);
        assertNotNull(winner);
        assertEquals(first.cid, winner.cid, "winning row must keep the first cid");
    }

    @Test
    void concurrentDuplicateCallsBothShortCircuit() throws Exception {
        // Race test: two concurrent identical calls used to blow up the loser on the unique
        // inputs_hash constraint because the lookup + insert path was non-atomic. With
        // tryInsert (ON CONFLICT DO NOTHING) the loser must instead see the winner's row and
        // return a Pending placeholder for the same cid.
        StubTokenService stub = new StubTokenService();
        stub.hold = true; // both calls' background work blocks so neither finalizes early
        TokenService proxied = wrap(stub, null);
        try {
            String ik = "ik-race-" + System.nanoTime();
            Asset a = asset("ast-RACE");
            java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.CompletableFuture<AssetCreationStatus> f1 = new java.util.concurrent.CompletableFuture<>();
            java.util.concurrent.CompletableFuture<AssetCreationStatus> f2 = new java.util.concurrent.CompletableFuture<>();
            Runnable call = () -> {
                try {
                    start.await();
                    f1.complete(proxied.createAsset(ik, a, null, null, null, null, null));
                } catch (Throwable t) { f1.completeExceptionally(t); }
            };
            Runnable call2 = () -> {
                try {
                    start.await();
                    f2.complete(proxied.createAsset(ik, a, null, null, null, null, null));
                } catch (Throwable t) { f2.completeExceptionally(t); }
            };
            new Thread(call).start();
            new Thread(call2).start();
            start.countDown();
            AssetCreationStatus r1 = f1.get(5, java.util.concurrent.TimeUnit.SECONDS);
            AssetCreationStatus r2 = f2.get(5, java.util.concurrent.TimeUnit.SECONDS);
            // Winner returns its own freshly-built Pending; loser hits resolveExisting and
            // returns a CachedJsonOperationStatus wrapping the persisted pending payload. Both
            // must carry the same cid.
            String c1 = extractCid(r1);
            String c2 = extractCid(r2);
            assertNotNull(c1, "first call must carry a cid, got " + r1.getClass());
            assertNotNull(c2, "second call must carry a cid, got " + r2.getClass());
            assertEquals(c1, c2, "both concurrent calls must converge on the same cid");
            assertEquals(1, stub.createCalls, "underlying service must run exactly once across the race");
        } finally {
            stub.released.countDown();
        }
    }

    @Test
    void duplicateCallAfterFailureReturnsFailurePayload() throws Exception {
        // Reviewer-flagged regression: a duplicate POST after the first call FAILED used to fall
        // through to a fresh Pending placeholder. Node returns operation.outputs verbatim for
        // any existing row, so the duplicate must immediately surface the original failure.
        StubTokenService stub = new StubTokenService();
        stub.failWith = new RuntimeException("boom");
        TokenService proxied = wrap(stub, null);

        String ik = "ik-fail-dup-" + System.nanoTime();
        Asset a = asset("ast-FAIL-DUP");

        AssetCreationStatus first = proxied.createAsset(ik, a, null, null, null, null, null);
        String cid = ((PendingAssetCreation) first).correlationId;

        // Wait for the row to reach FAILED.
        for (int i = 0; i < 100; i++) {
            OperationRecord r = store.findByCid(cid);
            if (r != null && r.status == OperationRecord.Status.FAILED) break;
            Thread.sleep(50);
        }

        // Stop the underlying service from throwing — a subsequent call must NOT re-run it.
        stub.failWith = null;
        AssetCreationStatus second = proxied.createAsset(ik, a, null, null, null, null, null);

        assertTrue(second instanceof io.ownera.ledger.adapter.service.workflow.CachedJsonOperationStatus,
                "duplicate after failure must return the cached marker wrapping stored JSON, got " + second.getClass());
        APIOperationStatusCreateAsset wrap = (APIOperationStatusCreateAsset)
                ((io.ownera.ledger.adapter.service.workflow.CachedJsonOperationStatus) second).apiStatus.getActualInstance();
        assertTrue(wrap.getOperation().getIsCompleted());
        assertNotNull(wrap.getOperation().getError(), "cached duplicate must carry the original failure payload");
        assertEquals(1, stub.createCalls, "underlying service must run exactly once across failure + duplicate");
    }

    @Test
    void duplicateCallShortCircuitsToExistingOutputs() throws Exception {
        // Duplicate POST on a COMPLETED row returns the stored outputs JSON byte-faithfully via
        // the CachedJsonOperationStatus marker. This is the Node createServiceProxy contract:
        // "if (!inserted) return storageOperation.outputs" — the wire response on a duplicate must
        // match the original, including fields that the internal-model round-trip would drop
        // (LedgerReference.network/standard, here).
        StubTokenService stub = new StubTokenService();
        TokenService proxied = wrap(stub, null);

        String ik = "ik-dedup-" + System.nanoTime();
        Asset a = asset("ast-DUP");

        AssetCreationStatus first = proxied.createAsset(ik, a, null, null, null, null, null);
        String cid = ((PendingAssetCreation) first).correlationId;

        // Wait for the first call to finalize.
        for (int i = 0; i < 50; i++) {
            OperationRecord r = store.findByCid(cid);
            if (r != null && r.status == OperationRecord.Status.COMPLETED) break;
            Thread.sleep(50);
        }

        AssetCreationStatus second = proxied.createAsset(ik, a, null, null, null, null, null);
        assertTrue(second instanceof io.ownera.ledger.adapter.service.workflow.CachedJsonOperationStatus,
                "duplicate must return the cached marker wrapping stored JSON, got " + second.getClass());
        assertEquals(1, stub.createCalls, "underlying service must run once across duplicate requests");

        // Byte-faithfulness: network + standard survive on the duplicate. The internal-model
        // round-trip (Mappers.fromAPI(create)) drops LedgerReference, so without the cached
        // marker these fields would come back as empty strings.
        APIOperationStatusCreateAsset wrap = (APIOperationStatusCreateAsset)
                ((io.ownera.ledger.adapter.service.workflow.CachedJsonOperationStatus) second).apiStatus.getActualInstance();
        APILedgerAssetIdentifierTypeCAIP19 caip19 = (APILedgerAssetIdentifierTypeCAIP19)
                wrap.getOperation().getResponse().getLedgerAssetInfo()
                        .getLedgerIdentifier().getActualInstance();
        assertEquals("hedera:testnet", caip19.getNetwork(),
                "network must round-trip on cached duplicate");
        assertEquals("HTS", caip19.getStandard(),
                "standard must round-trip on cached duplicate");
        assertEquals("tok-ast-DUP", caip19.getTokenId());
    }

    @Test
    void workflowArgsCodecKeepsUppercaseAssetTypeForHashStability() {
        // Cross-PR regression: PR 3 added a Node-style lowercase wire format for AssetType on
        // the /distribution/* HTTP surface. That fix must stay localized — the workflow proxy
        // hashes its serialized args into inputs_hash, so flipping the global wire shape would
        // make pre-upgrade rows un-findable (idempotent replay starts re-executing). The codec
        // must still serialize AssetType as the uppercase enum name.
        io.ownera.ledger.adapter.service.workflow.WorkflowArgsCodec codec =
                new io.ownera.ledger.adapter.service.workflow.WorkflowArgsCodec();
        String json = codec.encode(new Object[]{asset("ast-stable")});
        assertTrue(json.contains("\"FINP2P\""),
                "AssetType must serialize uppercase in workflow args (got " + json + ")");
        assertFalse(json.contains("\"finp2p\""),
                "AssetType must NOT serialize lowercase in workflow args (would break inputs_hash)");
    }

    @Test
    void nonProxiedMethodPassesThrough() {
        StubTokenService stub = new StubTokenService();
        TokenService proxied = wrap(stub, null);
        // getBalance is not in the proxied set — should call the target directly without
        // persisting a row.
        String balance = proxied.getBalance(asset("ast-B"), "fin-1");
        assertEquals("0", balance);
    }

    /** Pulls the cid out of either a freshly-built Pending (winner of an insert race) or a
     * CachedJsonOperationStatus marker (loser, or any duplicate hit). */
    private static String extractCid(AssetCreationStatus s) {
        if (s instanceof PendingAssetCreation) {
            return ((PendingAssetCreation) s).correlationId;
        }
        if (s instanceof io.ownera.ledger.adapter.service.workflow.CachedJsonOperationStatus) {
            APIOperationStatusCreateAsset wrap = (APIOperationStatusCreateAsset)
                    ((io.ownera.ledger.adapter.service.workflow.CachedJsonOperationStatus) s).apiStatus.getActualInstance();
            return wrap.getOperation().getCid();
        }
        return null;
    }

    @Test
    void recoveryReplaysPendingRowsAfterRestart() throws Exception {
        // Simulate a crash: build a proxy whose underlying service blocks forever, kick off a
        // call (row is now IN_PROGRESS with persisted inputs), then build a fresh proxy whose
        // service finalizes immediately, and run recovery — the original cid should now reach
        // COMPLETED via the new proxy.
        StubTokenService crashed = new StubTokenService();
        crashed.hold = true;
        TokenService crashedProxy = wrap(crashed, null);

        String ik = "ik-recovery-" + System.nanoTime();
        AssetCreationStatus pending = crashedProxy.createAsset(ik, asset("ast-R"), null, null, null, null, null);
        String cid = ((PendingAssetCreation) pending).correlationId;

        // Sanity: row is IN_PROGRESS.
        OperationRecord midflight = store.findByCid(cid);
        assertNotNull(midflight);
        assertEquals(OperationRecord.Status.IN_PROGRESS, midflight.status);

        // Restart: build a new service + new proxy handler as if after a JVM boot.
        StubTokenService restarted = new StubTokenService(); // finalizes immediately
        WorkflowServiceProxy handler = new WorkflowServiceProxy(
                restarted, store, null, Executors.newCachedThreadPool(),
                argsCodec, OperationOutputSerializer.defaultSerializer(),
                Set.of("createAsset", "issue", "transfer", "redeem"));
        WorkflowRecovery recovery = new WorkflowRecovery(store, handler,
                List.of("createAsset", "issue", "transfer", "redeem"), argsCodec);
        recovery.replayAll();

        // Wait for the replayed operation to finalize.
        OperationRecord finalRec = null;
        for (int i = 0; i < 100; i++) {
            finalRec = store.findByCid(cid);
            if (finalRec != null && finalRec.status == OperationRecord.Status.COMPLETED) break;
            Thread.sleep(50);
        }
        assertNotNull(finalRec);
        assertEquals(OperationRecord.Status.COMPLETED, finalRec.status,
                "recovery must finalize the IN_PROGRESS row");
        assertTrue(finalRec.result instanceof SuccessfulAssetCreation);
        assertEquals(1, restarted.createCalls,
                "recovery must call the (restarted) underlying service exactly once");

        // Quiet the original "crashed" worker so the test pool can shut down.
        crashed.released.countDown();
    }

}
