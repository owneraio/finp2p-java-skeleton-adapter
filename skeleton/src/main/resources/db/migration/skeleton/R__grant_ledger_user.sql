-- Grant schema access to the runtime user (LEDGER_USER).
-- Flyway placeholder ${ledger_user} is resolved from spring.flyway.placeholders.ledger_user.
-- If the placeholder is empty, this migration is a no-op.
-- Checks IF EXISTS to handle cases where schemas haven't been created yet.

DO
$$
DECLARE
    v_user TEXT := '${ledger_user}';
BEGIN
    IF v_user IS NOT NULL AND v_user <> '' THEN
        -- Skeleton tables (${schema_name} schema)
        IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = '${schema_name}') THEN
            EXECUTE format('GRANT USAGE ON SCHEMA ${schema_name} TO %I', v_user);
            EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA ${schema_name} TO %I', v_user);
            EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA ${schema_name} GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I', v_user);
        END IF;

        -- Adapter tables (public schema, where unqualified CREATE TABLE lands)
        IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'public') THEN
            EXECUTE format('GRANT USAGE ON SCHEMA public TO %I', v_user);
            EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO %I', v_user);
            EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I', v_user);
        END IF;
    END IF;
END
$$;
