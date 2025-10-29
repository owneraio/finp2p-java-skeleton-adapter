package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.PlanApprovalStatus;

public interface PlanApprovalService {
    PlanApprovalStatus approvePlan(String idempotencyKey, String planId);
}
