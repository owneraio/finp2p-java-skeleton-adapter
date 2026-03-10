package io.ownera.ledger.adapter.service.model;

public class FailedAssetCreation implements AssetCreationStatus {
    public final ErrorDetails details;

    public FailedAssetCreation(ErrorDetails details) {
        this.details = details;
    }
}
