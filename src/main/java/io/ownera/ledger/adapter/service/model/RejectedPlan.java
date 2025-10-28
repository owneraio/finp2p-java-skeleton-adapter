package io.ownera.ledger.adapter.service.model;

public class RejectedPlan implements PlanApprovalStatus {
    public final ErrorDetails details;

    public RejectedPlan(ErrorDetails details) {
        this.details = details;
    }
}
