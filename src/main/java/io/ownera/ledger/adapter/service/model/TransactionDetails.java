package io.ownera.ledger.adapter.service.model;

public class TransactionDetails {
    public final String transactionId;
    public final String operationId;

    public TransactionDetails(String transactionId, String operationId) {
        this.transactionId = transactionId;
        this.operationId = operationId;
    }
}
