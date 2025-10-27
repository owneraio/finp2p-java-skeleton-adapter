package io.ownera.ledger.adapter.service.model;

public class Destination {
    public final String finId;
    public final DestinationAccount account;

    public Destination(String finId, DestinationAccount account) {
        this.finId = finId;
        this.account = account;
    }
}
