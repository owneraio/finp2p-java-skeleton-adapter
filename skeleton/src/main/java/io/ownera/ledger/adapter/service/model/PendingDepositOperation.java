package io.ownera.ledger.adapter.service.model;

public class PendingDepositOperation implements DepositOperation {
    public final String correlationId;
    public final OperationMetadata metadata;

    public PendingDepositOperation(String correlationId, OperationMetadata metadata) {
        this.correlationId = correlationId;
        this.metadata = metadata;
    }

}
