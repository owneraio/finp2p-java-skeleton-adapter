package io.ownera.ledger.adapter.postgres;

import io.ownera.ledger.adapter.AbstractBusinessLogicTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Runs the full business logic test suite with separate migration and runtime DB users.
 * Verifies that the R__ grant migration correctly grants the runtime user access to both
 * ledger_adapter schema (skeleton tables) and public schema (adapter tables).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PostgresSeparateUsersTest extends AbstractBusinessLogicTest {

    private static final String RUNTIME_USER = "ledger";
    private static final String RUNTIME_PASSWORD = "ledger_pass";

    // Separate container using postgres superuser for migration
    private static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("finp2p_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static {
        PG.start();
        try (Connection conn = DriverManager.getConnection(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE USER " + RUNTIME_USER + " WITH PASSWORD '" + RUNTIME_PASSWORD + "'");
        } catch (Exception e) {
            if (!e.getMessage().contains("already exists")) {
                throw new RuntimeException("Failed to create runtime user", e);
            }
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Migration uses the postgres superuser
        registry.add("MIGRATION_CONNECTION_STRING", PG::getJdbcUrl);
        registry.add("MIGRATION_USERNAME", PG::getUsername);
        registry.add("MIGRATION_PASSWORD", PG::getPassword);

        // Runtime uses the limited user
        registry.add("DB_CONNECTION_STRING", PG::getJdbcUrl);
        registry.add("DB_USERNAME", () -> RUNTIME_USER);
        registry.add("DB_PASSWORD", () -> RUNTIME_PASSWORD);

        // LEDGER_USER tells Flyway grant script which user to grant
        registry.add("LEDGER_USER", () -> RUNTIME_USER);
    }
}
