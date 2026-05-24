package io.ownera.ledger.adapter.vanilla;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Verifies that the skeleton's {@code R__grant_ledger_user.sql} repeatable migration grants
 * <em>sequence</em> privileges in addition to table privileges, so a separate runtime role can
 * INSERT into vanilla-service's {@code accounts} table (which has a {@code BIGSERIAL} id column,
 * backed by an implicit sequence).
 *
 * <p>Without the sequence grant, the runtime role's first INSERT fails with
 * {@code permission denied for sequence accounts_id_seq}. The existing {@code AbstractBusinessLogicTest}
 * sub-classes in sample-adapter don't catch this because sample-adapter doesn't depend on
 * vanilla-service and therefore doesn't have any BIGSERIAL-backed tables. This test plugs that
 * gap in CI so the regression can't repeat silently.
 *
 * <p>Reuses the shared Testcontainers Postgres from {@link VanillaPostgresHolder} so the
 * container start cost is amortised across the suite, but installs everything (migrations,
 * runtime role, separate schema) inside an isolated namespace to avoid colliding with the
 * holder's existing fixtures.
 */
class VanillaSeparateUsersGrantTest {

    private static final String SCHEMA = "vanilla_separate_users_test";
    private static final String RUNTIME_USER = "vanilla_separate_users_runtime";
    private static final String RUNTIME_PASSWORD = "runtime-pw";

    @BeforeAll
    static void prepareSeparateUserDeployment() throws SQLException {
        // The shared container is started by VanillaPostgresHolder; reference it so JUnit's
        // ordering doesn't matter — touching the field forces class-init, which boots the
        // container if it isn't already up.
        VanillaPostgresHolder.POSTGRES.getJdbcUrl();

        try (Connection admin = adminDataSource().getConnection();
             Statement stmt = admin.createStatement()) {
            // Tear down any leftover state from a previous run so the migration's
            // CREATE-IF-NOT-EXISTS / ALTER DEFAULT PRIVILEGES paths exercise the
            // create-new-everything code-path each time.
            stmt.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
            stmt.execute("DROP ROLE IF EXISTS " + RUNTIME_USER);

            stmt.execute("CREATE ROLE " + RUNTIME_USER
                    + " WITH LOGIN PASSWORD '" + RUNTIME_PASSWORD + "'");
        }

        // Run the migrations as the admin user, telling the grant migration which role to grant.
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("schema_name", SCHEMA);
        placeholders.put("ledger_user", RUNTIME_USER);
        Flyway.configure()
                .dataSource(adminDataSource())
                .locations("classpath:db/migration/skeleton", "classpath:db/migration/vanilla")
                .placeholders(placeholders)
                .schemas(SCHEMA)
                .createSchemas(true)
                .load()
                .migrate();
    }

    @Test
    void runtimeUserCanInsertIntoBigserialBackedAccountsTable() throws SQLException {
        // Connect as the runtime role and execute an INSERT that exercises the implicit
        // accounts_id_seq sequence. Without sequence-level USAGE on the runtime role this
        // throws PSQLException("permission denied for sequence accounts_id_seq") — which is
        // exactly the production failure reported by SWIFT on 0.28.16.
        PGSimpleDataSource runtimeDs = runtimeDataSource();
        try (Connection conn = runtimeDs.getConnection();
             Statement stmt = conn.createStatement()) {
            assertDoesNotThrow(() ->
                    stmt.executeUpdate(
                            "INSERT INTO " + SCHEMA + ".accounts (fin_id, asset_id, asset_type) "
                                    + "VALUES ('fin-regression', 'asset-regression', 'finp2p')"),
                    "Runtime user must be granted sequence USAGE for INSERTs against the "
                            + "BIGSERIAL accounts.id column to succeed");
        }
    }

    @Test
    void runtimeUserCanInsertIntoAccountsCreatedAfterGrantMigration() throws SQLException {
        // ALTER DEFAULT PRIVILEGES protects future tables/sequences. Create a brand-new
        // BIGSERIAL-backed table as the admin role and verify the runtime user can INSERT
        // without the grant migration needing to re-run. This is what keeps the adapter from
        // having to bounce the grant migration after every new DDL.
        try (Connection admin = adminDataSource().getConnection();
             Statement stmt = admin.createStatement()) {
            stmt.execute("CREATE TABLE " + SCHEMA + ".future_table (id BIGSERIAL PRIMARY KEY, label TEXT)");
        }

        try (Connection conn = runtimeDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            assertDoesNotThrow(() ->
                    stmt.executeUpdate("INSERT INTO " + SCHEMA + ".future_table (label) VALUES ('regression')"),
                    "ALTER DEFAULT PRIVILEGES must extend sequence USAGE to tables created after "
                            + "the grant migration ran");
        }
    }

    private static PGSimpleDataSource adminDataSource() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(VanillaPostgresHolder.POSTGRES.getJdbcUrl());
        ds.setUser(VanillaPostgresHolder.POSTGRES.getUsername());
        ds.setPassword(VanillaPostgresHolder.POSTGRES.getPassword());
        return ds;
    }

    private static PGSimpleDataSource runtimeDataSource() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(VanillaPostgresHolder.POSTGRES.getJdbcUrl());
        ds.setUser(RUNTIME_USER);
        ds.setPassword(RUNTIME_PASSWORD);
        return ds;
    }
}
