package io.ownera.ledger.adapter.service.model;

public class AssetIdentifier {
    public final AssetIdentifierType type;
    public final String value;

    public AssetIdentifier(AssetIdentifierType type, String value) {
        this.type = type;
        this.value = value;
    }
}
