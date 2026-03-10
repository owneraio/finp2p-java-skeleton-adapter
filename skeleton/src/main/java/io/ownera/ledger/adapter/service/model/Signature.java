package io.ownera.ledger.adapter.service.model;

public class Signature {
    public final String signature;
    public final SignatureTemplate template;
    public final HashFunction hashFunction;

    public Signature(String signature, SignatureTemplate template, HashFunction hashFunction) {
        this.signature = signature;
        this.template = template;
        this.hashFunction = hashFunction;
    }
}
