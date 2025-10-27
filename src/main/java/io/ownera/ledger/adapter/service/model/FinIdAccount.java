package io.ownera.ledger.adapter.service.model;

public class FinIdAccount implements SourceAccount, DestinationAccount {
    public final String finId;

    public FinIdAccount(String finId) {
        this.finId = finId;
    }
}
