package io.ownera.ledger.adapter.service.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import javax.annotation.Nullable;

public class Asset {
    public final String assetId;
    public final AssetType assetType;
    @Nullable
    public final LedgerAssetIdentifier ledgerIdentifier;

    public Asset(String assetId, AssetType assetType) {
        this(assetId, assetType, null);
    }

    @JsonCreator
    public Asset(String assetId, AssetType assetType, @Nullable LedgerAssetIdentifier ledgerIdentifier) {
        this.assetId = assetId;
        this.assetType = assetType;
        this.ledgerIdentifier = ledgerIdentifier;
    }
}
