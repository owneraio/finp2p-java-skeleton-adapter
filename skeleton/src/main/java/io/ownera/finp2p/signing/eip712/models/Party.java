package io.ownera.finp2p.signing.eip712.models;

public class Party {
    private String idkey;

    public Party() {
    }

    public Party(String idkey) {
        this.idkey = idkey;
    }

    public String getIdkey() {
        return idkey;
    }

    public Party setIdkey(String idkey) {
        this.idkey = idkey;
        return this;
    }
}
