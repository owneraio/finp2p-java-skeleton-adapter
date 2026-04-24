package io.ownera.ledger.adapter.service.model;

import javax.annotation.Nullable;

/**
 * CAIP-19 ledger asset identifier.
 * Aligns with the Node.js skeleton's {@code Caip19LedgerAssetIdentifier} type.
 */
public class LedgerAssetIdentifier {
    public final String network;
    public final String tokenId;
    @Nullable
    public final String standard;

    public LedgerAssetIdentifier(String network, String tokenId, @Nullable String standard) {
        this.network = network;
        this.tokenId = tokenId;
        this.standard = standard;
    }
}
