package io.ownera.ledger.adapter.service.model;

public class PendingPlan implements PlanApprovalStatus {
    public final String correlationId;
    public final OperationMetadata metadata;

    public PendingPlan(String correlationId, OperationMetadata metadata) {
        this.correlationId = correlationId;
        this.metadata = metadata;
    }
}
