package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.PlanApprovalStatus;

public interface PlanApprovalService {

    PlanApprovalStatus approvePlan(String idempotencyKey, String planId);

    PlanApprovalStatus proposeCancelPlan(String idempotencyKey, String planId);

    PlanApprovalStatus proposeResetPlan(String idempotencyKey, String planId, int proposedSequence);

    PlanApprovalStatus proposeInstructionApproval(String idempotencyKey, String planId, int instructionSequence);

    void proposalStatus(String planId, String status, String requestType);
}
