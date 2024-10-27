package io.ownera.ledger.adapter.service.model;

public class ServiceSignatureField {
    public final String name;
    public final String type;
    public final String value;

    public ServiceSignatureField(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }
}
