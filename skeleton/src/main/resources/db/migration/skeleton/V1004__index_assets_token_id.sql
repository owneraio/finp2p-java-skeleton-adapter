-- Index ledger_adapter.assets.token_id for AssetStore.getByTokenId / existsByTokenId.
--
-- Adapters that honor AssetBind during createAsset need to look up an asset by its on-chain
-- tokenId to confirm it's registered locally (rather than silently echoing the supplied
-- tokenId back into a fresh SuccessfulAssetCreation). Without an index the lookup is a full
-- table scan over the assets table, which is small today but grows linearly with every
-- create_asset call.
--
-- Partial WHERE token_id <> '' excludes the empty-string backfill rows that V1002 left in
-- existing 0.27.x → 0.28 upgrades. Those rows aren't bound to any on-chain token and
-- shouldn't be returned by a tokenId lookup anyway.

CREATE INDEX IF NOT EXISTS assets_token_id_idx
    ON ${schema_name}.assets (token_id)
    WHERE token_id <> '';
