package io.ownera.finp2p.signing.eip712.models;

public class Asset {
    private String assetId;
    private String assetType;

    public Asset() {
    }

    public Asset(String assetId, String assetType) {
        this.assetId = assetId;
        this.assetType = assetType;
    }

    public String getAssetId() {
        return assetId;
    }

    public Asset setAssetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public String getAssetType() {
        return assetType;
    }

    public Asset setAssetType(String assetType) {
        this.assetType = assetType;
        return this;
    }
}
