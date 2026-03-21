package io.ownera.finp2p.signing.eip712.models;

public class Source {
    private String accountType;
    private String finId;

    public Source() {
    }

    public Source(String accountType, String finId) {
        this.accountType = accountType;
        this.finId = finId;
    }

    public String getAccountType() {
        return accountType;
    }

    public Source setAccountType(String accountType) {
        this.accountType = accountType;
        return this;
    }

    public String getFinId() {
        return finId;
    }

    public Source setFinId(String finId) {
        this.finId = finId;
        return this;
    }
}
