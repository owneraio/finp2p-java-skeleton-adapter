package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.PlanApprovalService;
import io.ownera.ledger.adapter.service.model.ApprovedPlan;
import io.ownera.ledger.adapter.service.model.PlanApprovalStatus;

public class AutoPlanApprovalService implements PlanApprovalService {
    @Override
    public PlanApprovalStatus approvePlan(String idempotencyKey, String planId) {
        return new ApprovedPlan();
    }

}
