package io.ownera.ledger.adapter.service.model;

public class SuccessfulDepositOperation implements DepositOperation {
    public final DepositInstruction depositInstruction;

    public SuccessfulDepositOperation(DepositInstruction depositInstruction) {
        this.depositInstruction = depositInstruction;
    }
}
