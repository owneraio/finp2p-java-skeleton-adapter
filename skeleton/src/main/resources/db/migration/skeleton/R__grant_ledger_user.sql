-- Grant ledger_adapter schema access to the runtime user (LEDGER_USER).
-- Flyway placeholder ${ledger_user} is resolved from spring.flyway.placeholders.ledger_user.
-- If the placeholder is empty, this migration is a no-op.

DO
$$
DECLARE
    v_user TEXT := '${ledger_user}';
BEGIN
    IF v_user IS NOT NULL AND v_user <> '' THEN
        EXECUTE format('GRANT USAGE ON SCHEMA ledger_adapter TO %I', v_user);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA ledger_adapter TO %I', v_user);
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA ledger_adapter GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I', v_user);
    END IF;
END
$$;
