package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import javax.annotation.Nullable;

public interface OperationStore {

    @Nullable
    OperationRecord findByInputsHash(String inputsHash);

    /**
     * Persist a new operation row with optional pending-payload JSON.
     *
     * <p>Mirrors the Node skeleton's {@code createServiceProxy} contract: every known CID has a
     * pollable payload from the moment it is created — pending while the operation is in flight,
     * then success/failure once it finalizes. Passing {@code null} leaves the {@code outputs}
     * column unset (useful for callers that have no payload to persist yet).
     */
    void save(OperationRecord record, @Nullable String pendingOutputsJson);

    /**
     * Backward-compatible overload that persists no pending payload. New callers should pass
     * a serialized pending {@link io.ownera.ledger.adapter.api.model.APIOperationStatus} so the
     * polling endpoint can return an in-progress payload for the cid.
     */
    default void save(OperationRecord record) {
        save(record, null);
    }

    /**
     * Persist a status transition with serialized outputs.
     *
     * <p>{@code outputsJson} should be the JSON of {@link io.ownera.ledger.adapter.api.model.APIOperationStatus}
     * (or {@code null} when no payload is available). PR 0 added this overload because the original
     * (cid, status, OperationStatus) signature silently dropped its result argument — the polling
     * endpoint and idempotent-replay path both depended on persisted outputs.
     */
    void updateStatus(String cid, OperationRecord.Status status, @Nullable String outputsJson);

    /**
     * Backward-compatible overload that ignores the in-memory {@code OperationStatus}.
     * New callers should serialize first and pass JSON via the {@code (cid, status, String)} overload.
     *
     * @deprecated kept for callers built before PR 0; will be removed in a future release.
     */
    @Deprecated
    default void updateStatus(String cid, OperationRecord.Status status, @Nullable OperationStatus result) {
        updateStatus(cid, status, (String) null);
    }

    @Nullable
    OperationRecord findByCid(String cid);

    /**
     * Read the persisted outputs JSON for a cid — the serialized
     * {@link io.ownera.ledger.adapter.api.model.APIOperationStatus}. Returns {@code null} when
     * the cid does not exist or the operation has not yet completed (no outputs persisted).
     */
    @Nullable
    String findOutputsByCid(String cid);
}
