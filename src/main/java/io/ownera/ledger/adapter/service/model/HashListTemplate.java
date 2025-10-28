package io.ownera.ledger.adapter.service.model;

import java.util.List;

public class HashListTemplate {
    public final List<HashGroup> hashGroups;
    public final String hash;

    public HashListTemplate(List<HashGroup> hashGroups, String hash) {
        this.hashGroups = hashGroups;
        this.hash = hash;
    }
}
