package io.ownera.ledger.adapter.service.asset;

import io.ownera.ledger.adapter.service.model.Asset;

import javax.annotation.Nullable;

/**
 * Persistence contract for the skeleton-owned {@code ledger_adapter.assets} table.
 * Adapters can persist an {@link Asset} (including its {@link io.ownera.ledger.adapter.service.model.LedgerAssetIdentifier})
 * during {@code createAsset} and look it up later — either by FinP2P {@code assetId} (the
 * primary key) or by the ledger-side {@code tokenId} (the on-chain handle the asset is bound
 * to). The latter is the lookup adapters need when {@code createAsset} receives an
 * {@code AssetBind} with a pre-existing {@code tokenIdentifier.tokenId} and the adapter wants
 * to confirm the bound asset is actually registered locally rather than blindly echoing the
 * tokenId back.
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

    /**
     * Look up an asset by the ledger-side {@code tokenId} carried on its
     * {@link io.ownera.ledger.adapter.service.model.LedgerAssetIdentifier}.
     *
     * <p>Returns {@code null} when no row matches — this is the signal adapters should treat
     * as "the bound asset isn't registered here" during {@code createAsset}.
     *
     * <p>A blank {@code tokenId} matches the empty-string backfill rows written by 0.27.x →
     * 0.28 migration and is treated as a miss (returns {@code null}). Callers should never
     * pass an empty string deliberately.
     *
     * @param tokenId non-null, non-empty ledger-side token id; nulls / empty strings return null.
     * @return the persisted asset, or {@code null} if no row matches.
     */
    @Nullable
    Asset getByTokenId(String tokenId);

    /**
     * @return {@code true} if any registered asset is bound to {@code tokenId}.
     */
    boolean existsByTokenId(String tokenId);
}
