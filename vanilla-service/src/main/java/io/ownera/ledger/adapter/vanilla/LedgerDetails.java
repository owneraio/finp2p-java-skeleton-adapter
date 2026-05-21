package io.ownera.ledger.adapter.vanilla;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

/**
 * Per-transaction details serialized into {@code transactions.details JSONB}.
 *
 * <p>{@code idempotencyKey} is the required de-duplication key — the storage layer's CTE-based
 * transfer checks for an existing row with the same {@code details->>'idempotency_key'} before
 * mutating any account, and Postgres' unique index on that expression catches duplicates that
 * slip past the CTE.
 *
 * <p>JSON property names use snake_case to match the Node vanilla service so adapters can mix
 * stores from either implementation against the same database.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class LedgerDetails {

    @JsonProperty("idempotency_key")
    public final String idempotencyKey;

    @JsonProperty("operation_id")
    public final @Nullable String operationId;

    @JsonProperty("operation_type")
    public final @Nullable String operationType;

    @JsonProperty("execution_context")
    public final @Nullable LedgerExecutionContext executionContext;

    @JsonProperty("transaction_id")
    public final @Nullable String transactionId;

    @JsonCreator
    public LedgerDetails(
            @JsonProperty("idempotency_key") String idempotencyKey,
            @JsonProperty("operation_id") @Nullable String operationId,
            @JsonProperty("operation_type") @Nullable String operationType,
            @JsonProperty("execution_context") @Nullable LedgerExecutionContext executionContext,
            @JsonProperty("transaction_id") @Nullable String transactionId) {
        this.idempotencyKey = idempotencyKey;
        this.operationId = operationId;
        this.operationType = operationType;
        this.executionContext = executionContext;
        this.transactionId = transactionId;
    }

    public LedgerDetails withIdempotencyKey(String newKey) {
        return new LedgerDetails(newKey, operationId, operationType, executionContext, transactionId);
    }

    public static final class LedgerExecutionContext {

        @JsonProperty("planId")
        public final String planId;

        @JsonProperty("sequence")
        public final int sequence;

        @JsonCreator
        public LedgerExecutionContext(
                @JsonProperty("planId") String planId,
                @JsonProperty("sequence") int sequence) {
            this.planId = planId;
            this.sequence = sequence;
        }
    }
}
