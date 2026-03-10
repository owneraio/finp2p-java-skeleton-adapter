package io.ownera.ledger.adapter.service.model;

public class SuccessfulAssetCreation implements AssetCreationStatus {
    public final AssetCreationResult result;

    public SuccessfulAssetCreation(AssetCreationResult result) {
        this.result = result;
    }
}
