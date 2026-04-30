package io.ownera.ledger.adapter.postgres;

import io.ownera.ledger.adapter.PostgresContainerHolder;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sanity check for the Node-skeleton-style schema-prefix convention: when each concrete
 * adapter sets its own default ({@code sample_adapter} here, {@code hedera}/{@code canton}
 * for real adapters), the legacy fallback {@code ledger_adapter} must NOT be created.
 *
 * Mirrors the Node PR #188 "custom schema name flows through migrations and queries" test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PostgresSchemaIsolationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_CONNECTION_STRING", PostgresContainerHolder.POSTGRES::getJdbcUrl);
        registry.add("DB_USERNAME", PostgresContainerHolder.POSTGRES::getUsername);
        registry.add("DB_PASSWORD", PostgresContainerHolder.POSTGRES::getPassword);
    }

    @Autowired
    private DSLContext dsl;

    @Test
    void migrationsCreateSampleAdapterSchemaButNotLedgerAdapter() {
        assertTrue(schemaExists("sample_adapter"),
                "sample_adapter schema must exist (concrete-adapter prefix is the default)");
        assertFalse(schemaExists("ledger_adapter"),
                "ledger_adapter (legacy fallback) must NOT exist when an adapter prefix is configured");
    }

    @Test
    void skeletonTablesLiveUnderTheConfiguredSchema() {
        assertTrue(tableExists("sample_adapter", "operations"));
        assertTrue(tableExists("sample_adapter", "assets"));
        assertTrue(tableExists("sample_adapter", "account_mappings"));
    }

    private boolean schemaExists(String name) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from("information_schema.schemata")
                        .where("schema_name = ?", name));
    }

    private boolean tableExists(String schema, String table) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from("information_schema.tables")
                        .where("table_schema = ? AND table_name = ?", schema, table));
    }
}
