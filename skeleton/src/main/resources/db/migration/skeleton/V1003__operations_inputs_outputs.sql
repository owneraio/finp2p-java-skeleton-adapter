-- Workflow correctness (PR 0): persist full inputs and outputs on operations.
--
-- Before this migration: only inputs_hash was stored, and outputs were never
-- persisted. OperationStore.findByCid always rebuilt with result=null, so
-- GET /api/operations/status/{cid} could never return the actual operation
-- result and the "return cached completed result" cache-hit path in
-- OperationExecutor.execute could never fire.
--
-- After: outputs hold the serialized APIOperationStatus JSON for the polling
-- endpoint to replay directly. inputs hold the full request payload for
-- inspection and future replay (parity with the Node skeleton's `inputs JSONB
-- UNIQUE` shape). inputs_hash is kept as a compatibility bridge for one
-- release; new code reads both and prefers inputs.

ALTER TABLE ${schema_name}.operations
    ADD COLUMN IF NOT EXISTS inputs  JSONB,
    ADD COLUMN IF NOT EXISTS outputs JSONB;

CREATE INDEX IF NOT EXISTS operations_status_idx
    ON ${schema_name}.operations (status);
