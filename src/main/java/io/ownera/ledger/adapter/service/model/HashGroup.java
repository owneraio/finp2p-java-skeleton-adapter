package io.ownera.ledger.adapter.service.model;

import java.util.List;

public class HashGroup {
    public final String hash;
    public final List<HashField> fields;

    public HashGroup(String hash, List<HashField> fields) {
        this.hash = hash;
        this.fields = fields;
    }
}
