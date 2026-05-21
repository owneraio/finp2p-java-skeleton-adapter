package io.ownera.ledger.adapter.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ownera.ledger.adapter.PostgresContainerHolder;
import io.ownera.ledger.adapter.api.model.APILedgerAssetIdentifierTypeCAIP19;
import io.ownera.ledger.adapter.api.model.APIOperationStatus;
import io.ownera.ledger.adapter.api.model.APIOperationStatusCreateAsset;
import io.ownera.ledger.adapter.service.workflow.DbOperationStore;
import io.ownera.ledger.adapter.service.workflow.OperationRecord;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the V1003 workflow-correctness fix.
 *
 * <p>Before V1003, {@code DbOperationStore.updateStatus(...)} silently dropped its
 * {@code OperationStatus} argument and {@code toRecord(...)} always rebuilt with
 * {@code result == null}. {@code GET /api/operations/status/{cid}} therefore could not
 * return the actual operation result — it delegated to {@code CommonService.operationStatus}
 * on the underlying adapter, which looks up by transaction id (wrong key for cid lookups)
 * and threw.
 *
 * <p>After V1003: the {@code outputs JSONB} column persists the serialized
 * {@link APIOperationStatus} produced by {@code Mappers.toAPI} at completion time, and the
 * polling endpoint reads that JSON directly — mirroring the Node skeleton's
 * {@code createServiceProxy} which returns {@code operation.outputs} verbatim
 * (see {@code finp2p-nodejs-skeleton-adapter/skeleton/src/workflows/service.ts}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PostgresWorkflowOutputsTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_CONNECTION_STRING", PostgresContainerHolder.POSTGRES::getJdbcUrl);
        registry.add("DB_USERNAME", PostgresContainerHolder.POSTGRES::getUsername);
        registry.add("DB_PASSWORD", PostgresContainerHolder.POSTGRES::getPassword);
    }

    @Autowired
    private DSLContext dsl;
    @Autowired
    private TestRestTemplate restTemplate;

    private OperationStore store;

    @BeforeEach
    void setup() {
        store = new DbOperationStore(dsl, "sample_adapter");
    }

    @Test
    void storeRoundTripsOutputsJson() throws Exception {
        String cid = "test-cid-roundtrip-" + System.nanoTime();
        store.save(new OperationRecord(cid, "createAsset", OperationRecord.Status.IN_PROGRESS,
                "hash-" + cid, null));

        // Before V1003 this call silently dropped the second argument.
        String outputs = "{\"type\":\"createAsset\",\"operation\":{\"cid\":\"\",\"isCompleted\":true}}";
        store.updateStatus(cid, OperationRecord.Status.COMPLETED, outputs);

        // Postgres JSONB normalizes whitespace; compare parsed trees, not byte-for-byte.
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(outputs), mapper.readTree(store.findOutputsByCid(cid)),
                "outputs JSON must round-trip through the store");
    }

    @Test
    void findOutputsByCidReturnsNullForUnknownCid() {
        assertNull(store.findOutputsByCid("does-not-exist-" + System.nanoTime()));
    }

    @Test
    void findOutputsByCidReturnsNullForInProgressOperation() {
        String cid = "test-cid-inprogress-" + System.nanoTime();
        store.save(new OperationRecord(cid, "issue", OperationRecord.Status.IN_PROGRESS,
                "hash-" + cid, null));
        // No updateStatus call → outputs column stays null.
        assertNull(store.findOutputsByCid(cid),
                "in-progress operation must not surface fake outputs");
    }

    @Test
    void pollingEndpointReturnsPersistedOutputsVerbatim() {
        // Stage a completed operation directly in the store so we can exercise the
        // polling endpoint end-to-end. (The sample sync executor completes inline
        // and the client never sees a cid; PR 1's async workflow will be the
        // production path that drives this endpoint.)
        String cid = "test-cid-polling-" + System.nanoTime();
        store.save(new OperationRecord(cid, "createAsset", OperationRecord.Status.IN_PROGRESS,
                "hash-" + cid, null));
        String outputs = "{"
                + "\"type\":\"createAsset\","
                + "\"operation\":{"
                + "\"cid\":\"\","
                + "\"isCompleted\":true,"
                + "\"response\":{\"ledgerAssetInfo\":{\"ledgerIdentifier\":{"
                + "\"assetIdentifierType\":\"CAIP-19\","
                + "\"network\":\"hedera:testnet\","
                + "\"tokenId\":\"0.0.123456\","
                + "\"standard\":\"HTS\""
                + "}}}"
                + "}"
                + "}";
        store.updateStatus(cid, OperationRecord.Status.COMPLETED, outputs);

        ResponseEntity<APIOperationStatus> resp = restTemplate.getForEntity(
                "/api/operations/status/" + cid, APIOperationStatus.class);
        assertEquals(200, resp.getStatusCodeValue());
        APIOperationStatus body = resp.getBody();
        assertNotNull(body);
        APIOperationStatusCreateAsset wrap = (APIOperationStatusCreateAsset) body.getActualInstance();
        assertEquals(APIOperationStatusCreateAsset.TypeEnum.CREATEASSET, wrap.getType());
        assertTrue(wrap.getOperation().getIsCompleted());
        APILedgerAssetIdentifierTypeCAIP19 caip19 = (APILedgerAssetIdentifierTypeCAIP19)
                wrap.getOperation().getResponse().getLedgerAssetInfo()
                        .getLedgerIdentifier().getActualInstance();
        assertEquals(APILedgerAssetIdentifierTypeCAIP19.AssetIdentifierTypeEnum.CAIP_19,
                caip19.getAssetIdentifierType());
        assertEquals("0.0.123456", caip19.getTokenId());
    }

    @Test
    void pollingEndpointReturns404ForUnknownCid() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/operations/status/does-not-exist-" + System.nanoTime(),
                String.class);
        assertEquals(404, resp.getStatusCodeValue());
    }

    @Test
    void findByInputsHashPopulatesResultFromStoredOutputs() {
        // The bug the reviewer flagged on PR 0: toRecord() always rebuilt with
        // result=null, so OperationExecutor.execute's cache-hit branch
        // (existing.result != null) could never fire. The fix populates result
        // by parsing the persisted outputs JSON via Mappers.fromAPI.
        String cid = "test-cid-replay-" + System.nanoTime();
        String inputsHash = "hash-" + cid;
        store.save(new OperationRecord(cid, "createAsset", OperationRecord.Status.IN_PROGRESS,
                inputsHash, null));

        // Persist a completed createAsset payload with a CAIP-19 ledgerIdentifier.
        String outputs = "{"
                + "\"type\":\"createAsset\","
                + "\"operation\":{"
                + "\"cid\":\"\","
                + "\"isCompleted\":true,"
                + "\"response\":{\"ledgerAssetInfo\":{\"ledgerIdentifier\":{"
                + "\"assetIdentifierType\":\"CAIP-19\","
                + "\"network\":\"hedera:testnet\","
                + "\"tokenId\":\"0.0.999999\","
                + "\"standard\":\"HTS\""
                + "}}}"
                + "}"
                + "}";
        store.updateStatus(cid, OperationRecord.Status.COMPLETED, outputs);

        OperationRecord found = store.findByInputsHash(inputsHash);
        assertNotNull(found, "record must be found by inputs hash");
        assertEquals(OperationRecord.Status.COMPLETED, found.status);
        assertNotNull(found.result,
                "result must be reconstructed from persisted outputs — this is the "
                        + "cache-hit fix; without it the executor falls through to Pending");
        // Verify the reconstructed result is the right concrete type and carries the tokenId.
        assertTrue(found.result instanceof io.ownera.ledger.adapter.service.model.SuccessfulAssetCreation,
                "expected SuccessfulAssetCreation, got " + found.result.getClass().getName());
        io.ownera.ledger.adapter.service.model.SuccessfulAssetCreation success =
                (io.ownera.ledger.adapter.service.model.SuccessfulAssetCreation) found.result;
        assertEquals("0.0.999999", success.result.tokenId,
                "tokenId must survive the JSONB round-trip");
    }

    @Test
    void findByCidPopulatesResultFromStoredOutputs() {
        // Same fix for the cid-keyed lookup used by other consumers.
        String cid = "test-cid-replay-cid-" + System.nanoTime();
        store.save(new OperationRecord(cid, "createAsset", OperationRecord.Status.IN_PROGRESS,
                "hash-" + cid, null));
        String outputs = "{"
                + "\"type\":\"createAsset\","
                + "\"operation\":{"
                + "\"cid\":\"\","
                + "\"isCompleted\":true,"
                + "\"response\":{\"ledgerAssetInfo\":{\"ledgerIdentifier\":{"
                + "\"assetIdentifierType\":\"CAIP-19\","
                + "\"network\":\"hedera:testnet\","
                + "\"tokenId\":\"0.0.42\","
                + "\"standard\":\"HTS\""
                + "}}}"
                + "}"
                + "}";
        store.updateStatus(cid, OperationRecord.Status.COMPLETED, outputs);

        OperationRecord found = store.findByCid(cid);
        assertNotNull(found);
        assertNotNull(found.result, "findByCid must also populate result from outputs");
        io.ownera.ledger.adapter.service.model.SuccessfulAssetCreation success =
                (io.ownera.ledger.adapter.service.model.SuccessfulAssetCreation) found.result;
        assertEquals("0.0.42", success.result.tokenId);
    }
}
