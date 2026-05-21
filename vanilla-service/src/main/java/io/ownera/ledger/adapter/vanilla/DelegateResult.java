package io.ownera.ledger.adapter.vanilla;

import javax.annotation.Nullable;

/**
 * Result of a delegate operation. Either a success carrying an external transaction id, or a
 * failure carrying an error message.
 *
 * <p>Pending / async results are intentionally unsupported — the delegate must complete its
 * external work synchronously before returning, so the vanilla service can decide whether to
 * commit or roll back the local DB safeguards (locks, etc.).
 *
 * <p>Mirrors Node's {@code DelegateResult} union in
 * {@code vanilla-service/src/interfaces.ts}.
 */
public final class DelegateResult {

    public final boolean success;
    public final @Nullable String transactionId;
    public final @Nullable String error;

    private DelegateResult(boolean success, @Nullable String transactionId, @Nullable String error) {
        this.success = success;
        this.transactionId = transactionId;
        this.error = error;
    }

    public static DelegateResult success(String transactionId) {
        return new DelegateResult(true, transactionId, null);
    }

    public static DelegateResult failure(String error) {
        return new DelegateResult(false, null, error);
    }
}
