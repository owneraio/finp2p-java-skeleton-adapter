package io.ownera.ledger.adapter.service.model;

public class FailedDepositOperation implements DepositOperation {
    public final ErrorDetails details;

    public FailedDepositOperation(ErrorDetails details) {
        this.details = details;
    }
}
