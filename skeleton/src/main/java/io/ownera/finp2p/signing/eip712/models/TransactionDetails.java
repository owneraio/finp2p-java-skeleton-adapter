package io.ownera.finp2p.signing.eip712.models;

public class TransactionDetails {
    private String operationId;
    private String transactionId;

    public TransactionDetails() {
    }

    public TransactionDetails(String operationId, String transactionId) {
        this.operationId = operationId;
        this.transactionId = transactionId;
    }

    public String getOperationId() {
        return operationId;
    }

    public TransactionDetails setOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public TransactionDetails setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }
}
