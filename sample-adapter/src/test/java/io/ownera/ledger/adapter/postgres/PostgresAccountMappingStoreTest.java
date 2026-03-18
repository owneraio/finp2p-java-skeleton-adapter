package io.ownera.ledger.adapter.postgres;

import io.ownera.ledger.adapter.PostgresContainerHolder;
import io.ownera.ledger.adapter.service.mapping.AccountMapping;
import io.ownera.ledger.adapter.service.mapping.JdbcAccountMappingStore;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JdbcAccountMappingStore: CRUD, idempotency, case-insensitive, ordering.
 * Aligned with Node.js skeleton's account-mappings.test.ts.
 */
public class PostgresAccountMappingStoreTest {

    private static JdbcAccountMappingStore store;
    private static DSLContext dsl;

    @BeforeAll
    static void setup() throws Exception {
        DataSource ds = new DriverManagerDataSource(
                PostgresContainerHolder.POSTGRES.getJdbcUrl(),
                PostgresContainerHolder.POSTGRES.getUsername(),
                PostgresContainerHolder.POSTGRES.getPassword());

        try (Connection conn = ds.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/migration/skeleton/V1001__create_ledger_adapter_schema.sql"));
        }

        dsl = DSL.using(ds, SQLDialect.POSTGRES);
        store = new JdbcAccountMappingStore(dsl);
    }

    @AfterEach
    void cleanup() {
        dsl.execute("DELETE FROM ledger_adapter.account_mappings");
    }

    @Test
    void shouldSaveAndRetrieveByFinId() {
        store.save("fin-1", "0xABC123");

        List<AccountMapping> mappings = store.getByFinId("fin-1");
        assertEquals(1, mappings.size());
        assertEquals("fin-1", mappings.get(0).getFinId());
        assertEquals("0xabc123", mappings.get(0).getAccount()); // lowercased
    }

    @Test
    void shouldSaveMultipleAccountsPerFinId() {
        store.save("fin-2", "account-a");
        store.save("fin-2", "account-b");

        List<AccountMapping> mappings = store.getByFinId("fin-2");
        assertEquals(2, mappings.size());
    }

    @Test
    void shouldBeIdempotentOnDuplicateInsert() {
        store.save("fin-3", "account-dup");
        store.save("fin-3", "account-dup"); // duplicate

        List<AccountMapping> mappings = store.getByFinId("fin-3");
        assertEquals(1, mappings.size(), "Duplicate insert should be idempotent");
    }

    @Test
    void shouldStoreLowercase() {
        store.save("fin-4", "0xABCDEF");

        List<AccountMapping> mappings = store.getByFinId("fin-4");
        assertEquals("0xabcdef", mappings.get(0).getAccount());
    }

    @Test
    void shouldRetrieveByAccount() {
        store.save("fin-5a", "shared-account");
        store.save("fin-5b", "shared-account");

        List<AccountMapping> mappings = store.getByAccount("shared-account");
        assertEquals(2, mappings.size());
    }

    @Test
    void shouldDeleteSpecificMapping() {
        store.save("fin-6", "account-x");
        store.save("fin-6", "account-y");

        store.delete("fin-6", "account-x");

        List<AccountMapping> mappings = store.getByFinId("fin-6");
        assertEquals(1, mappings.size());
        assertEquals("account-y", mappings.get(0).getAccount());
    }

    @Test
    void shouldDeleteAllMappingsForFinId() {
        store.save("fin-7", "account-1");
        store.save("fin-7", "account-2");

        store.delete("fin-7", null);

        List<AccountMapping> mappings = store.getByFinId("fin-7");
        assertTrue(mappings.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNonExistentFinId() {
        List<AccountMapping> mappings = store.getByFinId("nonexistent");
        assertTrue(mappings.isEmpty());
    }

    @Test
    void shouldListAll() {
        store.save("fin-8a", "acct-1");
        store.save("fin-8b", "acct-2");

        List<AccountMapping> all = store.listAll();
        assertTrue(all.size() >= 2);
    }
}
