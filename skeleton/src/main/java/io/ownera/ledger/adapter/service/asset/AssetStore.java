package io.ownera.ledger.adapter.service.asset;

import io.ownera.ledger.adapter.service.model.Asset;

import javax.annotation.Nullable;

/**
 * Persistence contract for the skeleton-owned {@code ledger_adapter.assets} table.
 * Adapters can persist an {@link Asset} (including its {@link io.ownera.ledger.adapter.service.model.LedgerAssetIdentifier})
 * during {@code createAsset} and look it up later via {@link #getById}.
 */
public interface AssetStore {

    /**
     * Insert the asset; idempotent (no-op on conflict).
     */
    void save(Asset asset);

    /**
     * @return the persisted asset (with its {@code LedgerAssetIdentifier} when present), or {@code null} if not registered.
     */
    @Nullable
    Asset getById(String assetId);

    /**
     * @return {@code true} if the asset is registered, {@code false} otherwise.
     */
    boolean exists(String assetId);
}
