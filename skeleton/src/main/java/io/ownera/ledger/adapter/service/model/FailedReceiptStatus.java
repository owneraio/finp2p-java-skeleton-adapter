package io.ownera.ledger.adapter.service.model;

public class FailedReceiptStatus implements ReceiptOperation {
    public final ErrorDetails details;

    public FailedReceiptStatus(ErrorDetails details) {
        this.details = details;
    }
}
