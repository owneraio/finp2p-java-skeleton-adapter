package io.ownera.ledger.adapter.service.model;

import java.util.List;

public class ServiceSignatureHashGroup {
    public final String hash;
    public final List<ServiceSignatureField> fields;

    public ServiceSignatureHashGroup(String hash, List<ServiceSignatureField> fields) {
        this.hash = hash;
        this.fields = fields;
    }
}
