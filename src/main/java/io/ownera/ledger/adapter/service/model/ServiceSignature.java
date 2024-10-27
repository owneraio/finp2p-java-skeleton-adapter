package io.ownera.ledger.adapter.service.model;

public class ServiceSignature {
    public final String signature;
    public final ServiceSignatureTemplate template;
    public final ServiceHashFunction hashFunction;

    public ServiceSignature(String signature, ServiceSignatureTemplate template, ServiceHashFunction hashFunction) {
        this.signature = signature;
        this.template = template;
        this.hashFunction = hashFunction;
    }
}
