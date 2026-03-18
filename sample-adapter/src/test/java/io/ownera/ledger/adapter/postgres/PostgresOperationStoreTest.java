package io.ownera.ledger.adapter.postgres;

import io.ownera.ledger.adapter.PostgresContainerHolder;
import io.ownera.ledger.adapter.sample.db.DbOperationStore;
import io.ownera.ledger.adapter.service.workflow.OperationRecord;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
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
 * Tests for DbOperationStore: idempotency, state transitions, crash recovery queries.
 * Aligned with Node.js skeleton's storage.test.ts.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostgresOperationStoreTest {

    private static OperationStore store;
    private static DSLContext dsl;

    @BeforeAll
    static void setup() throws Exception {
        DataSource ds = new DriverManagerDataSource(
                PostgresContainerHolder.POSTGRES.getJdbcUrl(),
                PostgresContainerHolder.POSTGRES.getUsername(),
                PostgresContainerHolder.POSTGRES.getPassword());

        // Run skeleton schema migration
        try (Connection conn = ds.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/migration/skeleton/V1001__create_ledger_adapter_schema.sql"));
        }

        dsl = DSL.using(ds, SQLDialect.POSTGRES);
        store = new DbOperationStore(dsl);

        // Clean operations table before tests
        dsl.execute("DELETE FROM ledger_adapter.operations");
    }

    @AfterEach
    void cleanup() {
        dsl.execute("DELETE FROM ledger_adapter.operations");
    }

    @Test
    void shouldSaveAndFindByCid() {
        OperationRecord record = new OperationRecord(
                "CID-100", "issue", OperationRecord.Status.IN_PROGRESS,
                "{\"method\":\"issue\",\"idempotencyKey\":\"ik-1\",\"data\":\"test\"}", null);

        store.save(record);

        OperationRecord found = store.findByCid("CID-100");
        assertNotNull(found);
        assertEquals("CID-100", found.cid);
        assertEquals("issue", found.method);
        assertEquals(OperationRecord.Status.IN_PROGRESS, found.status);
    }

    @Test
    void shouldFindByInputs() {
        String inputs = "{\"method\":\"transfer\",\"idempotencyKey\":\"ik-2\",\"data\":\"find-test\"}";
        OperationRecord record = new OperationRecord(
                "CID-200", "transfer", OperationRecord.Status.IN_PROGRESS, inputs, null);

        store.save(record);

        OperationRecord found = store.findByInputs(inputs);
        assertNotNull(found);
        assertEquals("CID-200", found.cid);
    }

    @Test
    void shouldReturnExistingOnDuplicateInputs() {
        String inputs = "{\"method\":\"issue\",\"idempotencyKey\":\"ik-dup\",\"data\":\"duplicate\"}";
        OperationRecord first = new OperationRecord(
                "CID-300", "issue", OperationRecord.Status.IN_PROGRESS, inputs, null);
        OperationRecord second = new OperationRecord(
                "CID-301", "issue", OperationRecord.Status.IN_PROGRESS, inputs, null);

        OperationRecord savedFirst = store.save(first);
        assertEquals("CID-300", savedFirst.cid);

        // Second save with same inputs should return original
        OperationRecord savedSecond = store.save(second);
        assertEquals("CID-300", savedSecond.cid, "Duplicate inputs should return original CID");
    }

    @Test
    void shouldUpdateStatusToSucceeded() {
        String inputs = "{\"method\":\"hold\",\"idempotencyKey\":\"ik-3\",\"data\":\"status-test\"}";
        OperationRecord record = new OperationRecord(
                "CID-400", "hold", OperationRecord.Status.IN_PROGRESS, inputs, null);
        store.save(record);

        String outputs = "{\"transactionId\":\"tx-123\"}";
        store.updateStatus("CID-400", OperationRecord.Status.SUCCEEDED, outputs);

        OperationRecord found = store.findByCid("CID-400");
        assertNotNull(found);
        assertEquals(OperationRecord.Status.SUCCEEDED, found.status);
        assertNotNull(found.outputs);
        assertTrue(found.outputs.contains("tx-123"));
    }

    @Test
    void shouldUpdateStatusToFailed() {
        String inputs = "{\"method\":\"redeem\",\"idempotencyKey\":\"ik-4\",\"data\":\"fail-test\"}";
        OperationRecord record = new OperationRecord(
                "CID-500", "redeem", OperationRecord.Status.IN_PROGRESS, inputs, null);
        store.save(record);

        store.updateStatus("CID-500", OperationRecord.Status.FAILED, null);

        OperationRecord found = store.findByCid("CID-500");
        assertNotNull(found);
        assertEquals(OperationRecord.Status.FAILED, found.status);
    }

    @Test
    void shouldFindPendingByMethod() {
        store.save(new OperationRecord("CID-601", "issue", OperationRecord.Status.IN_PROGRESS,
                "{\"method\":\"issue\",\"idempotencyKey\":\"ik-p1\",\"data\":\"pending1\"}", null));
        store.save(new OperationRecord("CID-602", "issue", OperationRecord.Status.IN_PROGRESS,
                "{\"method\":\"issue\",\"idempotencyKey\":\"ik-p2\",\"data\":\"pending2\"}", null));
        store.save(new OperationRecord("CID-603", "issue", OperationRecord.Status.SUCCEEDED,
                "{\"method\":\"issue\",\"idempotencyKey\":\"ik-p3\",\"data\":\"completed\"}", "{\"ok\":true}"));
        store.save(new OperationRecord("CID-604", "transfer", OperationRecord.Status.IN_PROGRESS,
                "{\"method\":\"transfer\",\"idempotencyKey\":\"ik-p4\",\"data\":\"other\"}", null));

        // Update the succeeded one
        store.updateStatus("CID-603", OperationRecord.Status.SUCCEEDED, "{\"ok\":true}");

        List<OperationRecord> pending = store.findPendingByMethod("issue");
        assertEquals(2, pending.size(), "Should find only in_progress operations for 'issue'");
        assertTrue(pending.stream().allMatch(r -> r.method.equals("issue")));
        assertTrue(pending.stream().allMatch(r -> r.status == OperationRecord.Status.IN_PROGRESS));
    }

    @Test
    void shouldReturnNullForNonExistentCid() {
        assertNull(store.findByCid("CID-NONEXISTENT"));
    }

    @Test
    void shouldReturnNullForNonExistentInputs() {
        assertNull(store.findByInputs("{\"nonexistent\":true}"));
    }
}
