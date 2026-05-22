package io.ownera.ledger.adapter.vanilla;

import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared Postgres container + DSLContext + ledger schema for vanilla-service tests.
 *
 * <p>Boots a Postgres container once per JVM (Testcontainers caches it), runs both the
 * skeleton's migrations (so the operations/account_mappings schema exists) and the vanilla
 * V3001 migration (accounts + transactions), and exposes a {@link DSLContext} the tests can
 * share. Each test isolates itself by using a freshly-namespaced schema or by inserting rows
 * with unique fin_ids.
 */
public final class VanillaPostgresHolder {

    public static final String SCHEMA = "vanilla_test";

    public static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("vanilla_test")
                    .withUsername("test")
                    .withPassword("test");

    public static final DSLContext CTX;

    static {
        POSTGRES.start();
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(POSTGRES.getJdbcUrl());
        ds.setUser(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());
        CTX = DSL.using(ds, SQLDialect.POSTGRES);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("schema_name", SCHEMA);
        // Skeleton's R__grant_ledger_user.sql expects a ledger_user; tests use the same role
        // that runs migrations (no separation of admin/runtime users at this layer).
        placeholders.put("ledger_user", POSTGRES.getUsername());
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration/skeleton", "classpath:db/migration/vanilla")
                .placeholders(placeholders)
                .load()
                .migrate();
    }

    private VanillaPostgresHolder() {}
}
