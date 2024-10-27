package io.ownera.ledger.adapter.service.model;

import java.util.List;

public class ServiceSignatureTemplate {
    public final List<ServiceSignatureHashGroup> hashGroups;
    public final String hash;

    public ServiceSignatureTemplate(List<ServiceSignatureHashGroup> hashGroups, String hash) {
        this.hashGroups = hashGroups;
        this.hash = hash;
    }
}
