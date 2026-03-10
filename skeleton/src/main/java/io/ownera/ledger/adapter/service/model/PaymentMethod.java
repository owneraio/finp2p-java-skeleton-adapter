package io.ownera.ledger.adapter.service.model;

public class PaymentMethod {
    public final String description;
    public final String currency;
    public final PaymentMethodInstruction methodInstruction;

    public PaymentMethod(String description, String currency, PaymentMethodInstruction methodInstruction) {
        this.description = description;
        this.currency = currency;
        this.methodInstruction = methodInstruction;
    }
}
