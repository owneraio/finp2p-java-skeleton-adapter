package io.ownera.ledger.adapter.service.model;

public class ExecutionContext {
    public final String planId;
    public final int sequence;

    public ExecutionContext(String planId, int sequence) {
        this.planId = planId;
        this.sequence = sequence;
    }
}
