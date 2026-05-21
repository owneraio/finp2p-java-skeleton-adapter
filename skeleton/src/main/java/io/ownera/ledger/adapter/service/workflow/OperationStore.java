package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import javax.annotation.Nullable;
import java.util.List;

public interface OperationStore {

    @Nullable
    OperationRecord findByInputsHash(String inputsHash);

    /**
     * Persist a new operation row with optional inputs and pending-payload JSON.
     *
     * <p>{@code inputsJson} is the serialized method-argument tuple used by the workflow proxy
     * for crash recovery (replay of {@code IN_PROGRESS} operations after restart). Pass
     * {@code null} when the caller does not own a re-invokable representation of its args.
     *
     * <p>{@code pendingOutputsJson} mirrors the Node skeleton's {@code createServiceProxy}
     * contract: every known CID has a pollable payload from the moment it is created — pending
     * while the operation is in flight, then success/failure once it finalizes.
     */
    void save(OperationRecord record, @Nullable String inputsJson, @Nullable String pendingOutputsJson);

    /**
     * Backward-compatible overload: no inputs to persist.
     */
    default void save(OperationRecord record, @Nullable String pendingOutputsJson) {
        save(record, null, pendingOutputsJson);
    }

    /**
     * Backward-compatible overload that persists no pending payload. New callers should pass
     * a serialized pending {@link io.ownera.ledger.adapter.api.model.APIOperationStatus} so the
     * polling endpoint can return an in-progress payload for the cid.
     */
    default void save(OperationRecord record) {
        save(record, null, null);
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

    /**
     * Fetch all {@code IN_PROGRESS} operations for a method, with their persisted input JSON.
     *
     * <p>Used by {@link WorkflowRecovery} at startup to replay operations that were left
     * in-flight when the process crashed. Mirrors Node's
     * {@code storage.getPendingOperations(method)} in {@code skeleton/src/workflows/storage.ts}.
     */
    List<PendingOperation> findPending(String method);

    /**
     * Row shape returned by {@link #findPending(String)}: the cid + persisted input args JSON
     * needed to re-invoke the original service method.
     */
    final class PendingOperation {
        public final String cid;
        public final String method;
        public final @Nullable String inputsJson;

        public PendingOperation(String cid, String method, @Nullable String inputsJson) {
            this.cid = cid;
            this.method = method;
            this.inputsJson = inputsJson;
        }
    }
}
