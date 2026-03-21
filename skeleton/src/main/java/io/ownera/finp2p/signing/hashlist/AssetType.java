package io.ownera.finp2p.signing.hashlist;

public enum AssetType {
    FINP2P("finp2p"),
    FIAT("fiat"),
    ;

    final String value;

    AssetType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
