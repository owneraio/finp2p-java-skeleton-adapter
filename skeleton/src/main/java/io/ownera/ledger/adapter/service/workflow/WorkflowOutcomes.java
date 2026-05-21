package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.model.ErrorDetails;
import io.ownera.ledger.adapter.service.model.FailedAssetCreation;
import io.ownera.ledger.adapter.service.model.FailedDepositOperation;
import io.ownera.ledger.adapter.service.model.FailedReceiptStatus;
import io.ownera.ledger.adapter.service.model.OperationMetadata;
import io.ownera.ledger.adapter.service.model.OperationStatus;
import io.ownera.ledger.adapter.service.model.PendingAssetCreation;
import io.ownera.ledger.adapter.service.model.PendingDepositOperation;
import io.ownera.ledger.adapter.service.model.PendingPlan;
import io.ownera.ledger.adapter.service.model.PendingReceiptStatus;
import io.ownera.ledger.adapter.service.model.RejectedPlan;

/**
 * Maps method names to their pending / failure {@link OperationStatus} variants.
 *
 * <p>Mirrors the Node skeleton's {@code wrappedResponse} switch
 * (skeleton/src/workflows/service.ts): each proxied method has both a pending and a failure
 * variant of the same status type. The workflow proxy uses this to build placeholders and
 * failure payloads without per-call lambdas.
 */
public final class WorkflowOutcomes {

    private WorkflowOutcomes() {}

    public static OperationStatus pendingFor(String method, String cid, OperationMetadata metadata) {
        switch (method) {
            case "createAsset":
                return new PendingAssetCreation(cid, metadata);
            case "issue":
            case "transfer":
            case "redeem":
            case "hold":
            case "release":
            case "rollback":
            case "payout":
                return new PendingReceiptStatus(cid, metadata);
            case "approvePlan":
            case "proposeCancelPlan":
            case "proposeResetPlan":
            case "proposeInstructionApproval":
                return new PendingPlan(cid, metadata);
            case "depositInstruction":      // legacy method-name string (sync OperationExecutor)
            case "getDepositInstruction":   // interface method name used by the workflow proxy
                return new PendingDepositOperation(cid, metadata);
            default:
                throw new IllegalArgumentException(
                        "Unknown method '" + method + "' — add it to WorkflowOutcomes.pendingFor()");
        }
    }

    public static OperationStatus failureFor(String method, int code, String message) {
        ErrorDetails details = new ErrorDetails(code, message);
        switch (method) {
            case "createAsset":
                return new FailedAssetCreation(details);
            case "issue":
            case "transfer":
            case "redeem":
            case "hold":
            case "release":
            case "rollback":
            case "payout":
                return new FailedReceiptStatus(details);
            case "approvePlan":
            case "proposeCancelPlan":
            case "proposeResetPlan":
            case "proposeInstructionApproval":
                return new RejectedPlan(details);
            case "depositInstruction":
            case "getDepositInstruction":
                return new FailedDepositOperation(details);
            default:
                throw new IllegalArgumentException(
                        "Unknown method '" + method + "' — add it to WorkflowOutcomes.failureFor()");
        }
    }
}
