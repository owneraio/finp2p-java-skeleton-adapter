package io.ownera.ledger.adapter.service.model;

import java.util.List;

public class DepositInstruction {
    public final Destination destination;
    public final String description;
    public final List<PaymentMethod> paymentOptions;
    public final String operationId;
    public final Object details;

    public DepositInstruction(Destination destination, String description, List<PaymentMethod> paymentOptions, String operationId, Object details) {
        this.destination = destination;
        this.description = description;
        this.paymentOptions = paymentOptions;
        this.operationId = operationId;
        this.details = details;
    }
}
