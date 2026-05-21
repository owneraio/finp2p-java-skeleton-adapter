package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.model.ErrorDetails;
import io.ownera.ledger.adapter.service.model.FailedAssetCreation;
import io.ownera.ledger.adapter.service.model.FailedDepositOperation;
import io.ownera.ledger.adapter.service.model.FailedReceiptStatus;
import io.ownera.ledger.adapter.service.model.OperationStatus;
import io.ownera.ledger.adapter.service.model.RejectedPlan;

/**
 * Maps method names to their failure {@link OperationStatus} variants.
 *
 * <p>Mirrors the Node skeleton's {@code wrappedResponse} switch
 * (skeleton/src/workflows/service.ts): each proxied method has both a pending and a failure
 * variant of the same status type. The executor uses this to persist a failure payload so the
 * polling endpoint can return a proper failed-operation response instead of 404.
 */
public final class WorkflowOutcomes {

    private WorkflowOutcomes() {}

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
                return new FailedDepositOperation(details);
            default:
                throw new IllegalArgumentException(
                        "Unknown method '" + method + "' — add it to WorkflowOutcomes.failureFor()");
        }
    }
}
