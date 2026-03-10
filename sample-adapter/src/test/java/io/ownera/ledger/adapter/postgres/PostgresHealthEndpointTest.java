package io.ownera.ledger.adapter.postgres;

import io.ownera.ledger.adapter.AbstractHealthEndpointTest;
import io.ownera.ledger.adapter.PostgresContainerHolder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.profiles.active=postgres"})
public class PostgresHealthEndpointTest extends AbstractHealthEndpointTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_CONNECTION_STRING", PostgresContainerHolder.POSTGRES::getJdbcUrl);
        registry.add("DB_USERNAME", PostgresContainerHolder.POSTGRES::getUsername);
        registry.add("DB_PASSWORD", PostgresContainerHolder.POSTGRES::getPassword);
    }
}
