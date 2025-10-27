package io.ownera.ledger.adapter.service.model;

public class Asset {
    public final String assetId;
    public final AssetType assetType;

    public Asset(String assetId, AssetType assetType) {
        this.assetId = assetId;
        this.assetType = assetType;
    }
}
