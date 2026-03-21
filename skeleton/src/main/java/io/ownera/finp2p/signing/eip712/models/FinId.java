package io.ownera.finp2p.signing.eip712.models;

public class FinId {
    private String idkey;

    public FinId() {}
    public FinId(String idkey) {
        this.idkey = idkey;
    }

    public String getIdkey() {
        return idkey;
    }

    public FinId setIdkey(String idkey) {
        this.idkey = idkey;
        return this;
    }
}
