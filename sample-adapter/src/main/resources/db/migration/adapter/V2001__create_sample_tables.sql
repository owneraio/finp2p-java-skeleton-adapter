-- Sample adapter schema: demo ledger tables (balances, hold operations, transactions)
-- These are specific to the sample adapter implementation, not part of the skeleton framework.

CREATE TABLE IF NOT EXISTS balances (
    fin_id   VARCHAR(512) NOT NULL,
    asset_id VARCHAR(512) NOT NULL,
    balance  INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (fin_id, asset_id)
);

CREATE TABLE IF NOT EXISTS hold_operations (
    operation_id VARCHAR(255) PRIMARY KEY,
    fin_id       VARCHAR(512) NOT NULL,
    quantity     VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
    id                  VARCHAR(255) PRIMARY KEY,
    source_fin_id       VARCHAR(512),
    destination_fin_id  VARCHAR(512),
    quantity            VARCHAR(50) NOT NULL,
    asset_id            VARCHAR(512) NOT NULL,
    asset_type          VARCHAR(50) NOT NULL,
    operation_type      VARCHAR(50) NOT NULL,
    operation_id        VARCHAR(255),
    execution_plan_id   VARCHAR(255),
    timestamp           BIGINT NOT NULL
);
