package io.ownera.ledger.adapter.service.model;

public class DepositAsset {
    public final String assetId;
    public final DepositAssetType assetType;

    public DepositAsset(String assetId, DepositAssetType assetType) {
        this.assetId = assetId;
        this.assetType = assetType;
    }
}
