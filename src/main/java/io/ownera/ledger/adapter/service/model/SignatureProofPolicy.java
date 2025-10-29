package io.ownera.ledger.adapter.service.model;

public class SignatureProofPolicy implements ProofPolicy {
    public final HashFunction hashFunction;
    public final SignatureTemplate template;
    public final String signature;

    public SignatureProofPolicy(HashFunction hashFunction, SignatureTemplate template, String signature) {
        this.hashFunction = hashFunction;
        this.template = template;
        this.signature = signature;
    }
}
