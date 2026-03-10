package io.ownera.ledger.adapter.service.model;

public class PaymentInstruction implements PaymentMethodInstruction {
   public final String instruction;

    public PaymentInstruction(String instruction) {
        this.instruction = instruction;
    }
}
