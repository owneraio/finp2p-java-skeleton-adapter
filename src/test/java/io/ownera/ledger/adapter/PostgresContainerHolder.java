package io.ownera.ledger.adapter;

import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresContainerHolder {

    public static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("finp2p_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }
}
