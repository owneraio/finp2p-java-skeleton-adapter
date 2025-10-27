package io.ownera.ledger.adapter.service.model;

public class AssetCreationResult {
    public final String tokenId;
    public final LedgerReference reference;

    public AssetCreationResult(String tokenId, LedgerReference reference) {
        this.tokenId = tokenId;
        this.reference = reference;
    }
}
