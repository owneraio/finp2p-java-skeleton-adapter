package io.ownera.ledger.adapter.vanilla;

import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.Destination;
import io.ownera.ledger.adapter.service.model.ExecutionContext;
import io.ownera.ledger.adapter.service.model.Source;

import javax.annotation.Nullable;

/**
 * Optional delegate for external escrow operations. When wired, the escrow service calls
 * hold/release/rollback on the delegate in addition to the local DB lock/unlock dance.
 *
 * <p>Ordering invariants the vanilla service enforces around delegate calls:
 * <ul>
 *   <li>{@code hold} — called <em>after</em> the local lock; if the delegate fails the local
 *       lock is released.</li>
 *   <li>{@code release} — called <em>before</em> the local unlockAndMove; if the delegate fails
 *       the funds stay held.</li>
 *   <li>{@code rollback} — called <em>before</em> the local unlock; if the delegate fails the
 *       funds stay held.</li>
 * </ul>
 *
 * <p>Mirrors Node's {@code EscrowDelegate} interface in
 * {@code vanilla-service/src/interfaces.ts}.
 */
public interface EscrowDelegate {

    DelegateResult hold(
            String idempotencyKey,
            Source source,
            @Nullable Destination destination,
            Asset asset,
            String quantity,
            String operationId,
            @Nullable ExecutionContext exCtx);

    DelegateResult release(
            String idempotencyKey,
            Source source,
            Destination destination,
            Asset asset,
            String quantity,
            String operationId,
            @Nullable ExecutionContext exCtx);

    DelegateResult rollback(
            String idempotencyKey,
            Source source,
            Asset asset,
            String quantity,
            String operationId,
            @Nullable ExecutionContext exCtx);
}
