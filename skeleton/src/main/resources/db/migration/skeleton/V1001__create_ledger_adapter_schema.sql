-- Skeleton framework schema: operations, assets, account_mappings
-- These tables are owned by the skeleton and shared across all adapter implementations.

CREATE SCHEMA IF NOT EXISTS ledger_adapter;

-- Operation tracking for idempotency and status polling
CREATE TABLE IF NOT EXISTS ledger_adapter.operations (
    cid         VARCHAR(255) PRIMARY KEY,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    method      VARCHAR(255) NOT NULL,
    status      VARCHAR(50)  NOT NULL,
    inputs_hash VARCHAR(255) NOT NULL UNIQUE
);

-- Asset registry (tokenId is fetched from OSS via finP2PSDK.getAsset())
CREATE TABLE IF NOT EXISTS ledger_adapter.assets (
    type           VARCHAR(255) NOT NULL,
    id             VARCHAR(255) NOT NULL,
    token_standard VARCHAR(50)  NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decimals       INTEGER      NOT NULL,
    PRIMARY KEY (type, id)
);

-- Account mappings: key-value pairs per finId (e.g. ledgerAccountId, custodyAccountId)
CREATE TABLE IF NOT EXISTS ledger_adapter.account_mappings (
    fin_id     VARCHAR(255) NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    value      VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (fin_id, field_name)
);

CREATE INDEX IF NOT EXISTS idx_account_mappings_fin_id
    ON ledger_adapter.account_mappings (fin_id);

CREATE INDEX IF NOT EXISTS idx_account_mappings_value
    ON ledger_adapter.account_mappings (value, field_name);
