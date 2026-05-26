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
     *
     * <p>{@code ctx.receipt} carries the router-side instruction completion (transaction id +
     * operation metadata) when the corresponding {@code instructionsCompletionEvents} entry
     * was a {@code ReceiptOutput}; {@code ctx.result} mirrors the same value in the legacy
     * receipt/error summary shape. Both may be {@code null} if the router hasn't reported a
     * completion event yet — adapters should treat that as "not ready" rather than "no
     * receipt".
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
        public final @Nullable InstructionReceipt receipt;

        /**
         * Back-compat constructor — no router-side completion event attached. Existing call
         * sites continue to compile; new sites should use the {@code receipt}-aware overload
         * below so adapters can read the {@code transactionId}, source/destination finIds, and
         * operation type without a separate {@code getReceipt} round-trip.
         */
        public InboundTransferContext(String planId, String source, Asset asset, String destination, String amount,
                                      int instructionSequence, @Nullable InstructionResult result) {
            this(planId, source, asset, destination, amount, instructionSequence, result, null);
        }

        public InboundTransferContext(String planId, String source, Asset asset, String destination, String amount,
                                      int instructionSequence,
                                      @Nullable InstructionResult result,
                                      @Nullable InstructionReceipt receipt) {
            super(planId, source, asset, destination, amount);
            this.instructionSequence = instructionSequence;
            this.result = result;
            this.receipt = receipt;
        }
    }

    /**
     * Lightweight receipt summary surfaced to {@link #onInboundTransfer}, derived from the
     * router's {@code instructionsCompletionEvents} entry for this instruction. Adapters that
     * need more than what's modelled here can use {@code transactionId} to look the full
     * receipt up via {@code FinP2PSDK.getReceipt(...)} — that's the canonical handle
     * on the router side.
     */
    class InstructionReceipt {
        /** Router-side receipt id (== local {@code transactionId}). */
        public final String transactionId;
        /** Operation type as reported by the router (e.g. {@code "issue"}, {@code "transfer"}). */
        public final String operationType;
        public final @Nullable String sourceFinId;
        public final @Nullable String destinationFinId;
        public final String quantity;

        public InstructionReceipt(String transactionId, String operationType,
                                  @Nullable String sourceFinId,
                                  @Nullable String destinationFinId,
                                  String quantity) {
            this.transactionId = transactionId;
            this.operationType = operationType;
            this.sourceFinId = sourceFinId;
            this.destinationFinId = destinationFinId;
            this.quantity = quantity;
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
