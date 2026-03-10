package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import javax.annotation.Nullable;

public class OperationRecord {

    public enum Status { IN_PROGRESS, COMPLETED, FAILED }

    public final String cid;
    public final String method;
    public final Status status;
    public final String inputsHash;
    public final @Nullable OperationStatus result;

    public OperationRecord(String cid, String method, Status status,
                           String inputsHash, @Nullable OperationStatus result) {
        this.cid = cid;
        this.method = method;
        this.status = status;
        this.inputsHash = inputsHash;
        this.result = result;
    }

    public OperationRecord withStatus(Status newStatus, @Nullable OperationStatus newResult) {
        return new OperationRecord(cid, method, newStatus, inputsHash, newResult);
    }
}
