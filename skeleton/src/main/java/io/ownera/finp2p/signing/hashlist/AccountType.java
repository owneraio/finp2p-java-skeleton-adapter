package io.ownera.finp2p.signing.hashlist;

public enum AccountType {
    FINID("finid"),
    ;

    final String value;

    AccountType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
