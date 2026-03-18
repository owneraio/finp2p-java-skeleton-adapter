package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.service.model.*;
import io.ownera.ledger.adapter.service.workflow.CorrelationIdGenerator;
import io.ownera.ledger.adapter.service.workflow.OperationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static io.ownera.ledger.adapter.Mappers.*;

@RestController
public class Controller {

    private final EscrowService escrowService;
    private final TokenService tokenService;
    private final PaymentService paymentService;
    private final PlanApprovalService planApprovalService;
    private final CommonService commonService;
    private final HealthService healthService;
    private final SignatureVerifier signatureVerifier;
    private final OperationExecutor operationExecutor;
    private final Optional<TransactionHook> transactionHook;

    public Controller(EscrowService escrowService, TokenService tokenService, PaymentService paymentService,
                      PlanApprovalService planApprovalService, CommonService commonService,
                      HealthService healthService, SignatureVerifier signatureVerifier,
                      OperationExecutor operationExecutor, Optional<TransactionHook> transactionHook) {
        this.escrowService = escrowService;
        this.tokenService = tokenService;
        this.paymentService = paymentService;
        this.planApprovalService = planApprovalService;
        this.commonService = commonService;
        this.healthService = healthService;
        this.signatureVerifier = signatureVerifier;
        this.operationExecutor = operationExecutor;
        this.transactionHook = transactionHook;
    }

    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    private static String ensureIdempotencyKey(String idempotencyKey) {
        return idempotencyKey != null ? idempotencyKey : CorrelationIdGenerator.generate();
    }

    // --- Health endpoints ---

    @GetMapping(value = "/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping(value = "/health/liveness")
    public ResponseEntity<String> liveness(
            @RequestHeader(name = "skip-vendor", required = false) String skipVendor
    ) {
        if (!"true".equals(skipVendor)) {
            healthService.liveness();
        }
        return ResponseEntity.ok("OK");
    }

    @GetMapping(value = "/health/readiness")
    public ResponseEntity<String> readiness(
            @RequestHeader(name = "skip-vendor", required = false) String skipVendor
    ) {
        if (!"true".equals(skipVendor)) {
            healthService.readiness();
        }
        return ResponseEntity.ok("OK");
    }

    // --- Execution plan endpoints ---

    @PostMapping(value = "/api/plan/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIApproveExecutionPlanResponse> approvePlan(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIApproveExecutionPlanRequest request
    ) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        String planId = request.getExecutionPlan().getId();
        logger.info("Approve plan: {}", planId);
        PlanApprovalStatus status = operationExecutor.execute(
                "approvePlan", ik, planId,
                () -> planApprovalService.approvePlan(ik, planId),
                cid -> new PendingPlan(cid, new OperationMetadata(new PollingResponseStrategy()))
        );
        return ResponseEntity.status(HttpStatus.OK).body(toAPIResponse(status));
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
            status = operationExecutor.execute(
                    "proposeCancelPlan", ik, planId,
                    () -> planApprovalService.proposeCancelPlan(ik, planId),
                    cid -> new PendingPlan(cid, new OperationMetadata(new PollingResponseStrategy()))
            );
        } else if (proposal instanceof APIExecutionPlanResetProposal) {
            APIExecutionPlanResetProposal reset = (APIExecutionPlanResetProposal) proposal;
            int seq = reset.getProposedSequence() != null ? reset.getProposedSequence() : 0;
            status = operationExecutor.execute(
                    "proposeResetPlan", ik, planId + ":" + seq,
                    () -> planApprovalService.proposeResetPlan(ik, planId, seq),
                    cid -> new PendingPlan(cid, new OperationMetadata(new PollingResponseStrategy()))
            );
        } else if (proposal instanceof APIExecutionPlanInstructionProposal) {
            APIExecutionPlanInstructionProposal instr = (APIExecutionPlanInstructionProposal) proposal;
            int seq = instr.getInstructionSequence() != null ? instr.getInstructionSequence() : 0;
            status = operationExecutor.execute(
                    "proposeInstructionApproval", ik, planId + ":" + seq,
                    () -> planApprovalService.proposeInstructionApproval(ik, planId, seq),
                    cid -> new PendingPlan(cid, new OperationMetadata(new PollingResponseStrategy()))
            );
        } else {
            logger.warn("Unknown proposal type: {}", proposal.getClass().getName());
            status = new ApprovedPlan();
        }

        return ResponseEntity.status(HttpStatus.OK).body(toAPIResponse(status));
    }

    @PostMapping(value = "/api/plan/proposal/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> proposalStatus(
            @RequestBody APIExecutionPlanProposalStatusRequest request
    ) {
        String planId = request.getRequest().getExecutionPlan().getId();
        String status = request.getStatus() != null ? request.getStatus().getValue() : "unknown";
        String requestType = request.getRequest().getExecutionPlan().getProposal() != null
                ? request.getRequest().getExecutionPlan().getProposal().getActualInstance().getClass().getSimpleName()
                : "unknown";
        logger.info("Proposal status: planId={}, status={}, type={}", planId, status, requestType);
        planApprovalService.proposalStatus(planId, status, requestType);
        return ResponseEntity.noContent().build();
    }

    // --- Asset management endpoints ---

    @PostMapping(value = "/api/assets/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APICreateAssetResponse> createAsset(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APICreateAssetRequest request
    ) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Create asset: {}", request);
        AssetCreationStatus status = operationExecutor.execute(
                "createAsset", ik, request.toString(),
                () -> tokenService.createAsset(
                        ik,
                        fromAPI(request.getAsset()),
                        fromAPI(request.getLedgerAssetBinding()),
                        request.getMetadata(),
                        request.getName(),
                        request.getIssuerId(),
                        fromAPI(request.getDenomination()),
                        fromAPI(request.getAssetIdentifier())
                ),
                cid -> new PendingAssetCreation(cid, new OperationMetadata(new PollingResponseStrategy()))
        );
        return ResponseEntity.status(HttpStatus.OK).body(toAPIResponse(status));
    }

    // --- Token operation endpoints ---

    @PostMapping(value = "/api/assets/issue", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> issue(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIIssueAssetsRequest request
    ) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Issue assets: {}", request);

        Asset asset = fromAPI(request.getAsset());
        FinIdAccount destination = fromAPI(request.getDestination());
        ExecutionContext exCtx = fromAPI(request.getExecutionContext());

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.ISSUE, null,
                destination.destination(), asset, request.getQuantity(), null, exCtx));
        ReceiptOperation rcptOp = operationExecutor.execute(
                "issue", ik, request.toString(),
                () -> tokenService.issue(ik, asset, destination, request.getQuantity(), exCtx),
                cid -> new PendingReceiptStatus(cid, new OperationMetadata(new PollingResponseStrategy()))
        );

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.ISSUE, null,
                destination.destination(), asset, request.getQuantity(), null, exCtx, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
    }

    @PostMapping(value = "/api/assets/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> transfer(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APITransferAssetRequest request
    ) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Transfer assets: {}", request);

        Source source = fromAPI(request.getSource());
        Destination destination = fromAPI(request.getDestination());
        Asset asset = fromAPI(request.getAsset());
        Signature sig = fromAPI(request.getSignature());
        ExecutionContext exCtx = fromAPI(request.getExecutionContext());

        // Uncomment to enable signature verification:
        // if (!signatureVerifier.verify(sig, request.getSource().getFinId())) {
        //     throw new BusinessException(4, "Signature verification failed");
        // }

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.TRANSFER, source, destination, asset,
                request.getQuantity(), sig, exCtx));
        ReceiptOperation rcptOp = operationExecutor.execute(
                "transfer", ik, request.toString(),
                () -> tokenService.transfer(ik, request.getNonce(), source, destination, asset,
                        request.getQuantity(), sig, exCtx),
                cid -> new PendingReceiptStatus(cid, new OperationMetadata(new PollingResponseStrategy()))
        );

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.TRANSFER, source, destination, asset,
                request.getQuantity(), sig, exCtx, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
    }

    @PostMapping(value = "/api/assets/redeem", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> redeem(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIRedeemAssetsRequest request
    ) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Redeem assets: {}", request);

        FinIdAccount source = fromAPI(request.getSource());
        Asset asset = fromAPI(request.getAsset());
        Signature sig = fromAPI(request.getSignature());
        ExecutionContext exCtx = fromAPI(request.getExecutionContext());

        // Uncomment to enable signature verification:
        // if (!signatureVerifier.verify(sig, request.getSource().getFinId())) {
        //     throw new BusinessException(4, "Signature verification failed");
        // }

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.REDEEM, source.source(), null, asset,
                request.getQuantity(), sig, exCtx));
        ReceiptOperation rcptOp = operationExecutor.execute(
                "redeem", ik, request.toString(),
                () -> tokenService.redeem(ik, request.getNonce(), source, asset,
                        request.getQuantity(), request.getOperationId(), sig, exCtx),
                cid -> new PendingReceiptStatus(cid, new OperationMetadata(new PollingResponseStrategy()))
        );

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.REDEEM, source.source(), null, asset,
                request.getQuantity(), sig, exCtx, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
    }

    // --- Balance endpoints ---

    @PostMapping(value = "/api/assets/getBalance", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIGetAssetBalanceResponse> getBalance(@RequestBody APIGetAssetBalanceRequest request) {
        Asset asset = fromAPI(request.getAsset());
        String balance = tokenService.getBalance(asset, request.getOwner().getFinId());
        return ResponseEntity.status(HttpStatus.OK).body(new APIGetAssetBalanceResponse()
                .asset(request.getAsset())
                .balance(balance));
    }

    @PostMapping(value = "/api/asset/balance", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIAssetBalanceInfoResponse> assetBalance(@RequestBody APIAssetBalanceInfoRequest request) {
        Asset asset = fromAPI(request.getAsset());
        String finId = request.getAccount().getFinId();
        Balance balance = tokenService.balance(asset, finId);
        return ResponseEntity.status(HttpStatus.OK).body(balanceToAPI(request.getAsset(), request.getAccount(), balance));
    }

    // --- Receipt & operation status endpoints ---

    @GetMapping(value = "/api/assets/receipts/{id}")
    public ResponseEntity<APIGetReceiptResponse> getReceipt(
            @PathVariable("id") String transactionId
    ) {
        ReceiptOperation receipt = commonService.getReceipt(transactionId);
        return ResponseEntity.status(HttpStatus.OK).body(Mappers.toAPIGetReceiptResponse(receipt));
    }

    @GetMapping(value = "/api/operations/status/{id}")
    public final ResponseEntity<APIOperationStatus> getOperationStatus(@PathVariable("id") String correlationId) {
        OperationStatus status = commonService.operationStatus(correlationId);
        return ResponseEntity.status(HttpStatus.OK).body(toAPI(status));
    }

    // --- Escrow endpoints ---

    @PostMapping(value = "/api/assets/hold", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> hold(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIHoldOperationRequest request
    ) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Hold assets: {}", request);

        Source source = fromAPI(request.getSource());
        Destination destination = fromAPI(request.getDestination());
        Asset asset = fromAPI(request.getAsset());
        Signature sig = fromAPI(request.getSignature());
        ExecutionContext exCtx = fromAPI(request.getExecutionContext());

        // Uncomment to enable signature verification:
        // if (!signatureVerifier.verify(sig, request.getSource().getFinId())) {
        //     throw new BusinessException(4, "Signature verification failed");
        // }

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.HOLD, source, destination, asset,
                request.getQuantity(), sig, exCtx));
        ReceiptOperation rcptOp = operationExecutor.execute(
                "hold", ik, request.toString(),
                () -> escrowService.hold(ik, request.getNonce(), source, destination, asset,
                        request.getQuantity(), sig, request.getOperationId(), exCtx),
                cid -> new PendingReceiptStatus(cid, new OperationMetadata(new PollingResponseStrategy()))
        );

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.HOLD, source, destination, asset,
                request.getQuantity(), sig, exCtx, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
    }

    @PostMapping(value = "/api/assets/release", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> release(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIReleaseOperationRequest request
    ) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Release assets: {}", request);

        Source source = fromAPI(request.getSource());
        Destination destination = fromAPI(request.getDestination());
        Asset asset = fromAPI(request.getAsset());
        ExecutionContext exCtx = fromAPI(request.getExecutionContext());

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.RELEASE, source, destination, asset,
                request.getQuantity(), null, exCtx));
        ReceiptOperation rcptOp = operationExecutor.execute(
                "release", ik, request.toString(),
                () -> escrowService.release(ik, source, destination, asset,
                        request.getQuantity(), request.getOperationId(), exCtx),
                cid -> new PendingReceiptStatus(cid, new OperationMetadata(new PollingResponseStrategy()))
        );

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.RELEASE, source, destination, asset,
                request.getQuantity(), null, exCtx, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
    }

    @PostMapping(value = "/api/assets/rollback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> rollback(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIRollbackOperationRequest request
    ) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Rollback assets: {}", request);

        Source source = fromAPI(request.getSource());
        Asset asset = fromAPI(request.getAsset());
        ExecutionContext exCtx = fromAPI(request.getExecutionContext());

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.ROLLBACK, source, null, asset,
                request.getQuantity(), null, exCtx));
        ReceiptOperation rcptOp = operationExecutor.execute(
                "rollback", ik, request.toString(),
                () -> escrowService.rollback(ik, source, asset,
                        request.getQuantity(), request.getOperationId(), exCtx),
                cid -> new PendingReceiptStatus(cid, new OperationMetadata(new PollingResponseStrategy()))
        );

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.ROLLBACK, source, null, asset,
                request.getQuantity(), null, exCtx, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
    }

    // --- Payment endpoints ---

    @PostMapping(value = "/api/payments/depositInstruction", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIDepositInstructionResponse> depositInstruction(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIDepositInstructionRequest request
    ) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Deposit instruction: {}", request);

        // Uncomment to enable signature verification:
        // Signature sig = fromAPI(request.getSignature());
        // if (sig != null && !signatureVerifier.verify(sig, request.getOwner().getFinId())) {
        //     throw new BusinessException(4, "Signature verification failed");
        // }
        DepositOperation rcptOp = operationExecutor.execute(
                "depositInstruction", ik, request.toString(),
                () -> paymentService.getDepositInstruction(
                        ik,
                        fromAPI(request.getOwner()),
                        fromAPI(request.getDestination()),
                        fromAPI(request.getAsset()),
                        request.getAmount(),
                        request.getDetails(),
                        request.getNonce(),
                        fromAPI(request.getSignature())
                ),
                cid -> new PendingDepositOperation(cid, new OperationMetadata(new PollingResponseStrategy()))
        );
        return ResponseEntity.status(HttpStatus.OK).body(toAPIResponse(rcptOp));
    }

    @PostMapping(value = "/api/payments/payout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIPayoutResponse> payout(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIPayoutRequest request
    ) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Payout: {}", request);

        Source source = fromAPI(request.getSource());
        Destination destination = fromAPI(request.getDestination());
        Asset asset = fromAPI(request.getAsset());
        Signature sig = fromAPI(request.getSignature());

        // Uncomment to enable signature verification:
        // if (sig != null && !signatureVerifier.verify(sig, request.getSource().getFinId())) {
        //     throw new BusinessException(4, "Signature verification failed");
        // }

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.TRANSFER, source, destination, asset,
                request.getQuantity(), sig, null));
        String description = null;
        if (request.getPayoutInstruction() != null) {
            description = request.getPayoutInstruction().getDescription();
        }
        String desc = description;
        ReceiptOperation rcptOp = operationExecutor.execute(
                "payout", ik, request.toString(),
                () -> paymentService.payout(ik, source, destination, asset,
                        request.getQuantity(), desc, request.getNonce(), sig),
                cid -> new PendingReceiptStatus(cid, new OperationMetadata(new PollingResponseStrategy()))
        );

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.TRANSFER, source, destination, asset,
                request.getQuantity(), sig, null, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPIPayoutResponse(rcptOp));
    }

}
