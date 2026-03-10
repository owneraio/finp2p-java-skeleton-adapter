package io.ownera.ledger.adapter.service.model;

public class AssetDenomination {
    public final AssetDenominationType type;
    public final String code;

    public AssetDenomination(AssetDenominationType type, String code) {
        this.type = type;
        this.code = code;
    }
}
