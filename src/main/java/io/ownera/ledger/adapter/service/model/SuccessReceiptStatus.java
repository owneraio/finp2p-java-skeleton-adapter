package io.ownera.ledger.adapter.service.model;

public class SuccessReceiptStatus implements ReceiptOperation {

    public final Receipt receipt;

    public SuccessReceiptStatus(Receipt receipt) {
        this.receipt = receipt;
    }
}
