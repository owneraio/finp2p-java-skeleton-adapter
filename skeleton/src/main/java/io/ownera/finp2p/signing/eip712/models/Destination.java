package io.ownera.finp2p.signing.eip712.models;

public class Destination {
    private String accountType;
    private String finId;

    public Destination() {
    }

    public Destination(String accountType, String finId) {
        this.accountType = accountType;
        this.finId = finId;
    }

    public String getAccountType() {
        return accountType;
    }

    public Destination setAccountType(String accountType) {
        this.accountType = accountType;
        return this;
    }

    public String getFinId() {
        return finId;
    }

    public Destination setFinId(String finId) {
        this.finId = finId;
        return this;
    }
}
