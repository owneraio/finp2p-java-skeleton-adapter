package io.ownera.ledger.adapter.service.workflow;

import javax.annotation.Nullable;
import java.util.List;

public interface OperationStore {

    /**
     * Find operation by inputs JSON. Used for idempotency dedup.
     */
    @Nullable
    OperationRecord findByInputs(String inputsJson);

    /**
     * Save a new operation. Should use ON CONFLICT(inputs) DO NOTHING
     * for idempotency — returns the existing record if inputs already exist.
     */
    OperationRecord save(OperationRecord record);

    /**
     * Update operation status and outputs.
     */
    void updateStatus(String cid, OperationRecord.Status status, @Nullable String outputsJson);

    /**
     * Find operation by CID.
     */
    @Nullable
    OperationRecord findByCid(String cid);

    /**
     * Find all in-progress operations for a given method. Used for crash recovery.
     */
    List<OperationRecord> findPendingByMethod(String method);
}
