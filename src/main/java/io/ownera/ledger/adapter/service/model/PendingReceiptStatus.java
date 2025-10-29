package io.ownera.ledger.adapter.service.model;

public class PendingReceiptStatus implements ReceiptOperation {
    public final String correlationId;
    public final OperationMetadata metadata;

    public PendingReceiptStatus(String correlationId, OperationMetadata metadata) {
        this.correlationId = correlationId;
        this.metadata = metadata;
    }

}
