package io.ownera.ledger.adapter.service.model;

public class FinIdAccount implements SourceAccount, DestinationAccount {
    public final String finId;

    public FinIdAccount(String finId) {
        this.finId = finId;
    }

    public Source source() {
        return new Source(finId, new FinIdAccount(finId));
    }

    public Destination destination() {
        return new Destination(finId, new FinIdAccount(finId));
    }
}
