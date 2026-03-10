-- Account mappings: finId ↔ ledger account association
-- Mirrors the Node.js skeleton's account_mappings table

CREATE TABLE IF NOT EXISTS account_mappings (
    fin_id    VARCHAR(255) NOT NULL,
    account   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (fin_id, account)
);

CREATE INDEX IF NOT EXISTS account_mappings_fin_id_idx
    ON account_mappings(fin_id, created_at, account);

CREATE INDEX IF NOT EXISTS account_mappings_account_idx
    ON account_mappings(account, created_at, fin_id);
