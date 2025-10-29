package io.ownera.ledger.adapter.service.model;

public class Source {
    public final String finId;
    public final SourceAccount account;

    public Source(String finId, SourceAccount account) {
        this.finId = finId;
        this.account = account;
    }
}
