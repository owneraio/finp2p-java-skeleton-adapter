package io.ownera.ledger.adapter.service.model.plan;

import io.ownera.ledger.adapter.service.model.FinIdAccount;

import javax.annotation.Nullable;

public final class RevertHoldInstruction implements ExecutionPlanOperation {
    @Nullable
    public final FinIdAccount source;
    public final FinIdAccount destination;

    public RevertHoldInstruction(@Nullable FinIdAccount source, FinIdAccount destination) {
        this.source = source;
        this.destination = destination;
    }
}
