-- Vanilla service per-investor ledger.
--
-- Assumes the schema already exists (created by the skeleton's V1001 migration).
-- Adds two tables:
--   * accounts      — (fin_id, asset_id, asset_type) UNIQUE; balance + held with CHECK
--                     constraints so held never exceeds balance.
--   * transactions  — append-only ledger of credits, debits, locks, moves, etc., with
--                     idempotency keyed off details->>'idempotency_key'.
--
-- Mirrors the Node vanilla-service migration (vanilla-service/migrations/20260310000000_add_vanilla_ledger.sql).

CREATE TABLE IF NOT EXISTS ${schema_name}.accounts (
    id         BIGSERIAL PRIMARY KEY,
    fin_id     VARCHAR(255) NOT NULL,
    asset_id   VARCHAR(255) NOT NULL,
    asset_type VARCHAR(64)  NOT NULL DEFAULT 'finp2p',
    balance    NUMERIC      NOT NULL DEFAULT 0,
    held       NUMERIC      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (fin_id, asset_id, asset_type),
    CHECK (balance >= 0),
    CHECK (held >= 0 AND held <= balance)
);

CREATE TABLE IF NOT EXISTS ${schema_name}.transactions (
    id               VARCHAR(50)  PRIMARY KEY,
    asset_id         VARCHAR(255) NOT NULL,
    asset_type       VARCHAR(64)  NOT NULL DEFAULT 'finp2p',
    source           VARCHAR(255),
    destination      VARCHAR(255),
    amount           NUMERIC      NOT NULL DEFAULT 0,
    source_held      NUMERIC      NOT NULL DEFAULT 0,
    destination_held NUMERIC      NOT NULL DEFAULT 0,
    action           VARCHAR(64)  NOT NULL,
    details          JSONB        NOT NULL DEFAULT '{}',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Idempotency: the storage layer's CTE checks for an existing tx by this expression before
-- mutating any account, so the same idempotency_key returns the original tx row.
CREATE UNIQUE INDEX IF NOT EXISTS tx_idempotency_idx
    ON ${schema_name}.transactions ((details->>'idempotency_key'));

CREATE INDEX IF NOT EXISTS tx_operation_idx
    ON ${schema_name}.transactions ((details->>'operation_id'));
