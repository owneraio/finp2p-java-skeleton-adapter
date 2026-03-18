package io.ownera.ledger.adapter.postgres;

import io.ownera.ledger.adapter.PostgresContainerHolder;
import io.ownera.ledger.adapter.sample.db.DbStorage;
import io.ownera.ledger.adapter.service.TokenServiceException;
import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.AssetType;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for sample adapter's DbStorage: credit/debit, balance constraints, hold operations.
 * Aligned with Node.js vanilla-service's storage.test.ts.
 */
public class PostgresStorageTest {

    private static DbStorage storage;
    private static DSLContext dsl;
    private static final Asset ASSET = new Asset("test-asset-1", AssetType.FINP2P);

    @BeforeAll
    static void setup() throws Exception {
        DataSource ds = new DriverManagerDataSource(
                PostgresContainerHolder.POSTGRES.getJdbcUrl(),
                PostgresContainerHolder.POSTGRES.getUsername(),
                PostgresContainerHolder.POSTGRES.getPassword());

        try (Connection conn = ds.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/migration/skeleton/V1001__create_ledger_adapter_schema.sql"));
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/migration/adapter/V2001__create_sample_tables.sql"));
        }

        dsl = DSL.using(ds, SQLDialect.POSTGRES);
        storage = new DbStorage(dsl);
    }

    @AfterEach
    void cleanup() {
        dsl.execute("DELETE FROM hold_operations");
        dsl.execute("DELETE FROM balances");
        dsl.execute("DELETE FROM ledger_adapter.assets");
    }

    private void createTestAsset() {
        storage.createAsset(ASSET.assetId, ASSET);
    }

    // --- Asset tests ---

    @Test
    void shouldCreateAsset() {
        createTestAsset();
        assertDoesNotThrow(() -> storage.checkAssetExists(ASSET));
    }

    @Test
    void shouldCreateAssetIdempotently() {
        createTestAsset();
        createTestAsset(); // duplicate
        assertDoesNotThrow(() -> storage.checkAssetExists(ASSET));
    }

    @Test
    void shouldThrowOnNonExistentAsset() {
        Asset missing = new Asset("nonexistent", AssetType.FINP2P);
        assertThrows(TokenServiceException.class, () -> storage.checkAssetExists(missing));
    }

    // --- Credit/Debit tests ---

    @Test
    void shouldCreditAndGetBalance() {
        createTestAsset();
        storage.credit("user-1", "100", ASSET);
        assertEquals("100", storage.getBalance("user-1", ASSET));
    }

    @Test
    void shouldCreditAccumulates() {
        createTestAsset();
        storage.credit("user-2", "50", ASSET);
        storage.credit("user-2", "30", ASSET);
        assertEquals("80", storage.getBalance("user-2", ASSET));
    }

    @Test
    void shouldDebitReducesBalance() {
        createTestAsset();
        storage.credit("user-3", "100", ASSET);
        storage.debit("user-3", "40", ASSET);
        assertEquals("60", storage.getBalance("user-3", ASSET));
    }

    @Test
    void shouldFailDebitInsufficientBalance() {
        createTestAsset();
        storage.credit("user-4", "50", ASSET);
        assertThrows(TokenServiceException.class, () -> storage.debit("user-4", "100", ASSET));
    }

    @Test
    void shouldReturnZeroForNonExistentBalance() {
        createTestAsset();
        assertEquals("0", storage.getBalance("nonexistent-user", ASSET));
    }

    // --- Move tests ---

    @Test
    void shouldMoveBalance() {
        createTestAsset();
        storage.credit("sender", "200", ASSET);
        storage.move("sender", "receiver", "75", ASSET);
        assertEquals("125", storage.getBalance("sender", ASSET));
        assertEquals("75", storage.getBalance("receiver", ASSET));
    }

    @Test
    void shouldFailMoveInsufficientBalance() {
        createTestAsset();
        storage.credit("sender-2", "50", ASSET);
        assertThrows(TokenServiceException.class,
                () -> storage.move("sender-2", "receiver-2", "100", ASSET));
    }

    // --- Hold operation tests ---

    @Test
    void shouldSaveAndGetHoldOperation() {
        storage.saveHoldOperation("op-1", "user-hold", "500");
        var hold = storage.getHoldOperation("op-1");
        assertNotNull(hold);
        assertEquals("user-hold", hold.finId);
        assertEquals("500", hold.quantity);
    }

    @Test
    void shouldRemoveHoldOperation() {
        storage.saveHoldOperation("op-2", "user-hold-2", "300");
        storage.removeHoldOperation("op-2");
        assertNull(storage.getHoldOperation("op-2"));
    }

    @Test
    void shouldReturnNullForNonExistentHold() {
        assertNull(storage.getHoldOperation("nonexistent-op"));
    }
}
