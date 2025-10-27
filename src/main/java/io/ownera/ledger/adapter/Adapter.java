package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.sample.InMemoryLedger;
import io.ownera.ledger.adapter.service.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
public class Adapter {

    private final static Logger logger = LoggerFactory.getLogger(Adapter.class);

    @Bean
    public LedgerService getLedgerService() {
        return new InMemoryLedger();
    }

    public static void main(String[] args) {
        SpringApplication.run(Adapter.class, args);
    }
}
