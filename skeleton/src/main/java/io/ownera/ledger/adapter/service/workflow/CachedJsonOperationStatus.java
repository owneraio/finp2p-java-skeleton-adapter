package io.ownera.ledger.adapter.service.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ownera.ledger.adapter.api.model.APIOperationStatus;
import io.ownera.ledger.adapter.service.model.AssetCreationStatus;
import io.ownera.ledger.adapter.service.model.DepositOperation;
import io.ownera.ledger.adapter.service.model.PlanApprovalStatus;
import io.ownera.ledger.adapter.service.model.ReceiptOperation;

/**
 * Marker {@link io.ownera.ledger.adapter.service.model.OperationStatus} carrying the raw stored
 * outputs JSON for a duplicate-call short-circuit.
 *
 * <p>The workflow proxy returns this on a cache hit. Mappers detect it and emit the API response
 * by unwrapping the stored {@link APIOperationStatus} directly, instead of round-tripping through
 * the lossy internal model. That preserves byte-faithfulness with the original POST's response —
 * fields like Receipt's {@code proof}, {@code LedgerReference}'s {@code network}/{@code standard},
 * deposit destinations, and payment options survive the duplicate hit.
 *
 * <p>Mirrors Node's {@code createServiceProxy} returning {@code storageOperation.outputs}
 * (skeleton/src/workflows/service.ts): the stored payload is the source of truth.
 *
 * <p>Implements all four service-method return marker interfaces so a single instance can satisfy
 * any proxied call site (createAsset / issue / transfer / ... / approvePlan / ...).
 */
public final class CachedJsonOperationStatus
        implements AssetCreationStatus, ReceiptOperation, DepositOperation, PlanApprovalStatus {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public final String rawJson;
    public final APIOperationStatus apiStatus;

    public CachedJsonOperationStatus(String rawJson) {
        this.rawJson = rawJson;
        try {
            this.apiStatus = MAPPER.readValue(rawJson, APIOperationStatus.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse stored outputs JSON for cached replay", e);
        }
    }
}
