package io.ownera.ledger.adapter.service.model;

public class PendingAssetCreation implements AssetCreationStatus {
    public final String correlationId;
    public final OperationMetadata metadata;

    public PendingAssetCreation(String correlationId, OperationMetadata metadata) {
        this.correlationId = correlationId;
        this.metadata = metadata;
    }
}
