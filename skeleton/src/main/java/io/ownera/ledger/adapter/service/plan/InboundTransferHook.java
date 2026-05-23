package io.ownera.ledger.adapter.service.plan;

import io.ownera.ledger.adapter.service.model.Asset;

import javax.annotation.Nullable;

/**
 * Hook for inbound transfer notifications during plan execution.
 *
 * <p><strong>Rejecting:</strong> throw {@link InboundTransferRejection} to deliberately reject
 * the plan with a supplied error {@code code} / {@code message}. Any other exception is
 * treated as a hook bug — warn-logged, otherwise ignored — so unrelated failures don't
 * silently fail-close legitimate plans.
 */
public interface InboundTransferHook {

    /**
     * Called during plan approval when a planned inbound transfer is detected. Throw
     * {@link InboundTransferRejection} to reject the plan.
     */
    void onPlannedInboundTransfer(String idempotencyKey, PlannedInboundTransferContext ctx);

    /**
     * Called after an instruction-level proposal is executed for an inbound transfer. Throw
     * {@link InboundTransferRejection} to reject the proposal.
     */
    void onInboundTransfer(String idempotencyKey, InboundTransferContext ctx);

    class PlannedInboundTransferContext {
        public final String planId;
        public final String source;
        public final Asset asset;
        public final String destination;
        public final String amount;

        public PlannedInboundTransferContext(String planId, String source, Asset asset, String destination, String amount) {
            this.planId = planId;
            this.source = source;
            this.asset = asset;
            this.destination = destination;
            this.amount = amount;
        }
    }

    class InboundTransferContext extends PlannedInboundTransferContext {
        public final int instructionSequence;
        public final @Nullable InstructionResult result;

        public InboundTransferContext(String planId, String source, Asset asset, String destination, String amount,
                                     int instructionSequence, @Nullable InstructionResult result) {
            super(planId, source, asset, destination, amount);
            this.instructionSequence = instructionSequence;
            this.result = result;
        }
    }

    class InstructionResult {
        public enum Type { RECEIPT, ERROR }
        public final Type type;
        public final @Nullable String transactionId;
        public final int code;
        public final @Nullable String message;

        public static InstructionResult receipt(String transactionId) {
            return new InstructionResult(Type.RECEIPT, transactionId, 0, null);
        }

        public static InstructionResult error(int code, String message) {
            return new InstructionResult(Type.ERROR, null, code, message);
        }

        private InstructionResult(Type type, @Nullable String transactionId, int code, @Nullable String message) {
            this.type = type;
            this.transactionId = transactionId;
            this.code = code;
            this.message = message;
        }
    }
}
