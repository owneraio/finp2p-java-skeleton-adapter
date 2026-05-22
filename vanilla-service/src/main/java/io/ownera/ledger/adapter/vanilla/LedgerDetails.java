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

    /** External transaction id returned by a delegate (e.g. on-chain tx hash). The
     *  service uses this both in the immediate response receipt and when later
     *  reconstructing a receipt via {@code CommonService.getReceipt}. */
    @JsonProperty("transaction_id")
    public final @Nullable String transactionId;

    /** External source counterparty (finId + non-FinId account info) preserved when the source
     *  side is a wallet rather than a vanilla-tracked account. */
    @JsonProperty("source_account")
    public final @Nullable LedgerAccountRef sourceAccount;

    /** External destination counterparty, same role as {@link #sourceAccount}. The vanilla
     *  storage row for an external transfer only carries the local-side finId, so the
     *  destination finId would be lost without this. */
    @JsonProperty("destination_account")
    public final @Nullable LedgerAccountRef destinationAccount;

    @JsonCreator
    public LedgerDetails(
            @JsonProperty("idempotency_key") String idempotencyKey,
            @JsonProperty("operation_id") @Nullable String operationId,
            @JsonProperty("operation_type") @Nullable String operationType,
            @JsonProperty("execution_context") @Nullable LedgerExecutionContext executionContext,
            @JsonProperty("transaction_id") @Nullable String transactionId,
            @JsonProperty("source_account") @Nullable LedgerAccountRef sourceAccount,
            @JsonProperty("destination_account") @Nullable LedgerAccountRef destinationAccount) {
        this.idempotencyKey = idempotencyKey;
        this.operationId = operationId;
        this.operationType = operationType;
        this.executionContext = executionContext;
        this.transactionId = transactionId;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
    }

    /** Convenience constructor — five-arg form, no external account info. */
    public LedgerDetails(String idempotencyKey, @Nullable String operationId,
                         @Nullable String operationType, @Nullable LedgerExecutionContext executionContext,
                         @Nullable String transactionId) {
        this(idempotencyKey, operationId, operationType, executionContext, transactionId, null, null);
    }

    public LedgerDetails withIdempotencyKey(String newKey) {
        return new LedgerDetails(newKey, operationId, operationType, executionContext,
                transactionId, sourceAccount, destinationAccount);
    }

    /** External counterparty descriptor preserved in JSONB so a stored row can rebuild the
     *  Source/Destination pair on a later receipt lookup. {@code finId} is the counterparty's
     *  fin id (the storage row's source/destination columns only hold the local side, so the
     *  remote finId would otherwise be lost). {@code type + address} preserve a non-FinId
     *  account shape (e.g. {@code LedgerAccount("wallet", "0xabc")}). */
    public static final class LedgerAccountRef {

        @JsonProperty("fin_id")
        public final String finId;

        @JsonProperty("type")
        public final String type;

        @JsonProperty("address")
        public final String address;

        @JsonCreator
        public LedgerAccountRef(
                @JsonProperty("fin_id") String finId,
                @JsonProperty("type") String type,
                @JsonProperty("address") String address) {
            this.finId = finId;
            this.type = type;
            this.address = address;
        }
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
