package io.ownera.ledger.adapter.vanilla;

import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.Destination;
import io.ownera.ledger.adapter.service.model.ExecutionContext;
import io.ownera.ledger.adapter.service.model.Source;

import javax.annotation.Nullable;

/**
 * Delegate for external transfer operations (on-chain transfers, IBAN payouts).
 *
 * <p>The vanilla service wraps each call with local DB safeguards:
 * <ol>
 *   <li>Lock funds in the local ledger.</li>
 *   <li>Call {@link #outboundTransfer}.</li>
 *   <li>On success → unlock and debit locally.</li>
 *   <li>On failure → unlock locally (funds return to available).</li>
 * </ol>
 *
 * <p>Mirrors Node's {@code TransferDelegate} interface in
 * {@code vanilla-service/src/interfaces.ts}.
 */
public interface TransferDelegate {

    DelegateResult outboundTransfer(
            String idempotencyKey,
            Source source,
            Destination destination,
            Asset asset,
            String quantity,
            @Nullable ExecutionContext exCtx);

    /**
     * Called before crediting a destination account for an inbound transfer. Implementations
     * should verify that the on-chain transfer actually happened; throw
     * {@link InboundTransferVerificationError} to abort the local credit.
     *
     * <p>Default no-op: adapters that trust their inbound sources can leave this unimplemented.
     */
    default void onInboundTransfer(
            String transactionId,
            Source source,
            Asset asset,
            Destination destination,
            String amount,
            @Nullable ExecutionContext exCtx) {
        // no-op
    }
}
