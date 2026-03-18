package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.PlanApprovalService;
import io.ownera.ledger.adapter.service.model.ApprovedPlan;
import io.ownera.ledger.adapter.service.model.PlanApprovalStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple plan approval service that auto-approves everything.
 * Production adapters should use DefaultPlanApprovalService with a plugin.
 */
public class AutoPlanApprovalService implements PlanApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(AutoPlanApprovalService.class);

    @Override
    public PlanApprovalStatus approvePlan(String idempotencyKey, String planId) {
        return new ApprovedPlan();
    }

    @Override
    public PlanApprovalStatus proposeCancelPlan(String idempotencyKey, String planId) {
        logger.info("Auto-approve cancel proposal: {}", planId);
        return new ApprovedPlan();
    }

    @Override
    public PlanApprovalStatus proposeResetPlan(String idempotencyKey, String planId, int proposedSequence) {
        logger.info("Auto-approve reset proposal: {} to sequence {}", planId, proposedSequence);
        return new ApprovedPlan();
    }

    @Override
    public PlanApprovalStatus proposeInstructionApproval(String idempotencyKey, String planId, int instructionSequence) {
        logger.info("Auto-approve instruction proposal: {} sequence {}", planId, instructionSequence);
        return new ApprovedPlan();
    }

    @Override
    public void proposalStatus(String planId, String status, String requestType) {
        logger.info("Proposal status: plan={}, status={}, type={}", planId, status, requestType);
    }
}
