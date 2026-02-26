package io.ownera.ledger.adapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ownera.ledger.adapter.sample.JdbcLedger;
import io.ownera.ledger.adapter.sample.JdbcOperationStore;
import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.service.proof.ProofProvider;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
import io.ownera.ledger.adapter.service.workflow.OperationTrackingCommonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

@Configuration
@Profile("postgres")
public class PostgresConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(PostgresConfiguration.class);

    @Bean
    public JdbcLedger jdbcLedger(JdbcTemplate jdbcTemplate, Optional<ProofProvider> proofProvider) {
        logger.info("Initializing JdbcLedger with PostgreSQL backend");
        return new JdbcLedger(jdbcTemplate, proofProvider.orElse(null));
    }

    @Bean
    public TokenService tokenService(JdbcLedger ledger) {
        return ledger;
    }

    @Bean
    public EscrowService escrowService(JdbcLedger ledger) {
        return ledger;
    }

    @Bean
    public CommonService commonService(JdbcLedger ledger, OperationStore operationStore) {
        return new OperationTrackingCommonService(ledger, operationStore);
    }

    @Bean
    public OperationStore operationStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new JdbcOperationStore(jdbcTemplate, objectMapper);
    }
}
