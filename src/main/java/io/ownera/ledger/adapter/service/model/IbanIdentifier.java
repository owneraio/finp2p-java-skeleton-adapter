package io.ownera.ledger.adapter.service.model;

public class IbanIdentifier implements DestinationAccount {
    public final String code;

    public IbanIdentifier(String code) {
        this.code = code;
    }
}
