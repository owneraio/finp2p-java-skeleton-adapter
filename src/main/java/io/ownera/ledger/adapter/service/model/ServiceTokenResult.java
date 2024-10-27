package io.ownera.ledger.adapter.service.model;


public class ServiceTokenResult {
    public final String transactionId;
    public final String assetId;
    public final String sourceFinId;
    public final String destinationFinId;
    public final String quantity;

    public ServiceTokenResult(String transactionId, String assetId, String sourceFinId, String destinationFinId, String quantity) {
        this.transactionId = transactionId;
        this.assetId = assetId;
        this.sourceFinId = sourceFinId;
        this.destinationFinId = destinationFinId;
        this.quantity = quantity;
    }
}
