package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.PlanApprovalStatus;
import io.ownera.ledger.adapter.service.model.PlanProposal;
import io.ownera.ledger.adapter.service.model.ProposalStatus;

public interface PlanApprovalService {

    PlanApprovalStatus approvePlan(String idempotencyKey, String planId);

    PlanApprovalStatus proposeCancelPlan(String idempotencyKey, String planId);

    PlanApprovalStatus proposeResetPlan(String idempotencyKey, String planId, int proposedSequence);

    PlanApprovalStatus proposeInstructionApproval(String idempotencyKey, String planId, int instructionSequence);

    /**
     * Notification that a previously-returned proposal outcome has been acknowledged by the router.
     *
     * @param planId   the execution plan ID
     * @param proposal the typed proposal that was raised (cancel/reset/instruction)
     * @param status   outcome (approved/rejected)
     */
    void proposalStatus(String planId, PlanProposal proposal, ProposalStatus status);
}
