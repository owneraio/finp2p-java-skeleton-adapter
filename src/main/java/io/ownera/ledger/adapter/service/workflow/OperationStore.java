package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import javax.annotation.Nullable;

public interface OperationStore {

    @Nullable
    OperationRecord findByInputsHash(String inputsHash);

    void save(OperationRecord record);

    void updateStatus(String cid, OperationRecord.Status status, @Nullable OperationStatus result);

    @Nullable
    OperationRecord findByCid(String cid);
}
