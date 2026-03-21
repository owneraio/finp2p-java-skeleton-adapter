package io.ownera.finp2p.signing.hashlist;

public enum Operation {
    ISSUE("issue"),
    TRANSFER("transfer"),
    REDEEM("redeem"),
    DEPOSIT("deposit"),
    WITHDRAW("withdraw"),
    ;

    final String value;

    Operation(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
