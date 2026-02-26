package io.ownera.ledger.adapter.postgres;

import io.ownera.ledger.adapter.AbstractInsufficientBalanceTest;
import io.ownera.ledger.adapter.PostgresContainerHolder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.profiles.active=postgres"})
public class PostgresInsufficientBalanceTest extends AbstractInsufficientBalanceTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresContainerHolder.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresContainerHolder.POSTGRES::getUsername);
        registry.add("spring.datasource.password", PostgresContainerHolder.POSTGRES::getPassword);
    }
}
