package io.ownera.finp2p.skeleton.web;

import io.ownera.ledger.adapter.Mappers;
import io.ownera.ledger.adapter.api.model.APIApproveExecutionPlanRequest;
import io.ownera.ledger.adapter.api.model.APIApproveExecutionPlanResponse;
import io.ownera.ledger.adapter.api.model.APIExecutionPlanCancellationProposal;
import io.ownera.ledger.adapter.api.model.APIExecutionPlanInstructionProposal;
import io.ownera.ledger.adapter.api.model.APIExecutionPlanProposalRequest;
import io.ownera.ledger.adapter.api.model.APIExecutionPlanProposalStatusRequest;
import io.ownera.ledger.adapter.api.model.APIExecutionPlanResetProposal;
import io.ownera.ledger.adapter.service.PlanApprovalService;
import io.ownera.ledger.adapter.service.model.ApprovedPlan;
import io.ownera.ledger.adapter.service.model.PlanApprovalStatus;
import io.ownera.ledger.adapter.service.model.PlanProposal;
import io.ownera.ledger.adapter.service.model.ProposalStatus;
import io.ownera.ledger.adapter.service.workflow.CorrelationIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /api/plan/*} routes — only loaded when a {@link PlanApprovalService} bean exists.
 *
 * <p>Adapters that don't participate in plan approval simply omit the bean; Spring evaluates
 * {@link ConditionalOnBean} at context startup, so the routes never appear in the URL space.
 * Requests to {@code /api/plan/approve} on such an adapter come back as 404, accurately
 * reflecting the adapter's contract.
 */
@RestController
public class PlanApprovalController {

    private static final Logger logger = LoggerFactory.getLogger(PlanApprovalController.class);

    private final PlanApprovalService planApprovalService;

    public PlanApprovalController(PlanApprovalService planApprovalService) {
        this.planApprovalService = planApprovalService;
    }

    @PostMapping(value = "/api/plan/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIApproveExecutionPlanResponse> approvePlan(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIApproveExecutionPlanRequest request
    ) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        String planId = request.getExecutionPlan().getId();
        logger.info("Approve plan: {}", planId);
        PlanApprovalStatus status = planApprovalService.approvePlan(ik, planId);
        return ResponseEntity.status(HttpStatus.OK).body(Mappers.toAPIResponse(status));
    }

    @PostMapping(value = "/api/plan/proposal", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIApproveExecutionPlanResponse> planProposal(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIExecutionPlanProposalRequest request
    ) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        String planId = request.getExecutionPlan().getId();
        Object proposal = request.getExecutionPlan().getProposal().getActualInstance();
        logger.info("Plan proposal: planId={}, type={}", planId, proposal.getClass().getSimpleName());

        PlanApprovalStatus status;
        if (proposal instanceof APIExecutionPlanCancellationProposal) {
            status = planApprovalService.proposeCancelPlan(ik, planId);
        } else if (proposal instanceof APIExecutionPlanResetProposal) {
            APIExecutionPlanResetProposal reset = (APIExecutionPlanResetProposal) proposal;
            int seq = reset.getProposedSequence() != null ? reset.getProposedSequence() : 0;
            status = planApprovalService.proposeResetPlan(ik, planId, seq);
        } else if (proposal instanceof APIExecutionPlanInstructionProposal) {
            APIExecutionPlanInstructionProposal instr = (APIExecutionPlanInstructionProposal) proposal;
            int seq = instr.getInstructionSequence() != null ? instr.getInstructionSequence() : 0;
            status = planApprovalService.proposeInstructionApproval(ik, planId, seq);
        } else {
            logger.warn("Unknown proposal type: {}", proposal.getClass().getName());
            status = new ApprovedPlan();
        }
        return ResponseEntity.status(HttpStatus.OK).body(Mappers.toAPIResponse(status));
    }

    @PostMapping(value = "/api/plan/proposal/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> proposalStatus(@RequestBody APIExecutionPlanProposalStatusRequest request) {
        String planId = request.getRequest().getExecutionPlan().getId();
        PlanProposal proposal = Mappers.proposalFromAPI(request.getRequest().getExecutionPlan().getProposal());
        ProposalStatus status = "rejected".equalsIgnoreCase(
                request.getStatus() != null ? request.getStatus().getValue() : null)
                ? ProposalStatus.REJECTED : ProposalStatus.APPROVED;
        logger.info("Proposal status: planId={}, status={}, type={}", planId, status, proposal.getClass().getSimpleName());
        planApprovalService.proposalStatus(planId, proposal, status);
        return ResponseEntity.noContent().build();
    }

    static String ensureIdempotencyKey(String idempotencyKey) {
        return idempotencyKey != null ? idempotencyKey : CorrelationIdGenerator.generate();
    }
}
