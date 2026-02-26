package io.ownera.ledger.adapter;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.profiles.active=in-memory"})
public class EscrowOperationsTest extends AbstractEscrowOperationsTest {
}
