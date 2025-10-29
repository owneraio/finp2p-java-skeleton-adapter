package io.ownera.ledger.adapter.sample;

public class HoldOperation {
    public final String finId;
    public final String quantity;
    public HoldOperation(String finId, String quantity) {
        this.finId = finId;
        this.quantity = quantity;
    }
}
