-- Persist the CAIP-19 ledger identifier on assets.
-- Since 0.28, the API carries asset.ledgerIdentifier (network + tokenId + standard)
-- which the skeleton's internal Asset model also exposes. Adapters need to persist
-- these so subsequent operations (issue/transfer/redeem) can return the same
-- identifier on the receipt response (the FinP2P node rejects null identifiers
-- with code 999 "unknown discriminator value").
--
-- Existing rows backfill to empty string (matches the runtime fallback in
-- Mappers.toAPILedgerIdentifier when Asset.ledgerIdentifier is null).

ALTER TABLE ${schema_name}.assets
    ADD COLUMN IF NOT EXISTS token_id VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS network  VARCHAR(255) NOT NULL DEFAULT '';
