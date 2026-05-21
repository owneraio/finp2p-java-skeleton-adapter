package io.ownera.ledger.adapter.vanilla;

import io.ownera.ledger.adapter.service.model.AssetBind;
import io.ownera.ledger.adapter.service.model.AssetCreationResult;
import io.ownera.ledger.adapter.service.model.AssetDenomination;

import javax.annotation.Nullable;

/**
 * Optional delegate for asset creation. When provided, {@code VanillaServiceImpl.createAsset}
 * calls the delegate instead of generating a local tokenId, so adapters that need to mint
 * on an external chain can route through here.
 *
 * <p>Mirrors Node's {@code AssetDelegate} interface in {@code vanilla-service/src/interfaces.ts}.
 */
public interface AssetDelegate {

    AssetCreationResult createAsset(
            String idempotencyKey,
            String assetId,
            @Nullable AssetBind assetBind,
            @Nullable Object assetMetadata,
            @Nullable String assetName,
            @Nullable String issuerId,
            @Nullable AssetDenomination assetDenomination);
}
