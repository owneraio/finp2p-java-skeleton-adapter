package io.ownera.ledger.adapter.service.model.plan;

public final class AwaitInstruction implements ExecutionPlanOperation {
    public final long waitUntil;

    public AwaitInstruction(long waitUntil) {
        this.waitUntil = waitUntil;
    }
}
