-- Skeleton framework schema: operations, assets, account_mappings
-- These tables are owned by the skeleton and shared across all adapter implementations.

CREATE SCHEMA IF NOT EXISTS ledger_adapter;

-- CID sequence for operation correlation IDs
CREATE SEQUENCE IF NOT EXISTS ledger_adapter.operation_cid_seq;

-- Operation tracking for idempotency and status polling
CREATE TABLE IF NOT EXISTS ledger_adapter.operations (
    cid        VARCHAR(255) PRIMARY KEY DEFAULT ('CID-' || nextval('ledger_adapter.operation_cid_seq')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    method     VARCHAR(255) NOT NULL,
    status     VARCHAR(50)  NOT NULL,
    inputs     JSONB        NOT NULL UNIQUE,
    outputs    JSONB        NOT NULL DEFAULT '{}'
);

CREATE INDEX IF NOT EXISTS idx_operations_status
    ON ledger_adapter.operations (status);

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

-- Investor account mappings (many-to-many: finId <-> account)
CREATE TABLE IF NOT EXISTS ledger_adapter.account_mappings (
    fin_id     VARCHAR(255) NOT NULL,
    account    VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (fin_id, account)
);

CREATE INDEX IF NOT EXISTS idx_account_mappings_fin_id
    ON ledger_adapter.account_mappings (fin_id, created_at, account);

CREATE INDEX IF NOT EXISTS idx_account_mappings_account
    ON ledger_adapter.account_mappings (account, created_at, fin_id);
