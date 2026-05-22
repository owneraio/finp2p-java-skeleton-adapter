package io.ownera.ledger.adapter.vanilla;

import io.ownera.ledger.adapter.service.model.AssetType;

/**
 * Reads the on-chain balance of the omnibus wallet for a given asset.
 *
 * <p>{@link DistributionService} requires a delegate — the on-chain balance is the source of
 * truth that the local omnibus row is reconciled against, so the vanilla service cannot
 * function as a distribution-tracking adapter without it.
 *
 * <p>Mirrors Node's {@code OmnibusDelegate} interface in
 * {@code vanilla-service/src/interfaces.ts}.
 */
public interface OmnibusDelegate {

    /**
     * @return the on-chain balance of the omnibus account for {@code (assetId, assetType)},
     *         encoded as a numeric string (same format as the storage layer uses).
     */
    String getOmnibusBalance(String assetId, AssetType assetType);
}
