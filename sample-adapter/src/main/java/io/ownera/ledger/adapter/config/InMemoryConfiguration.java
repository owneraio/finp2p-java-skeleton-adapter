package io.ownera.ledger.adapter.config;

import io.ownera.ledger.adapter.sample.inmemory.InMemoryLedger;
import io.ownera.ledger.adapter.sample.inmemory.InMemoryOperationStore;
import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.service.proof.ProofProvider;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
import io.ownera.ledger.adapter.service.workflow.OperationTrackingCommonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Optional;

@Configuration
@Profile("in-memory")
public class InMemoryConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryConfiguration.class);

    @Bean
    public InMemoryLedger inMemoryLedger(
            @Value("${ORG_ID}") String orgId,
            Optional<ProofProvider> proofProvider) {
        logger.info("Initializing InMemoryLedger with ORG_ID: {}", orgId);
        return new InMemoryLedger(proofProvider.orElse(null));
    }

    @Bean
    public TokenService tokenService(InMemoryLedger ledger) {
        return ledger;
    }

    @Bean
    public EscrowService escrowService(InMemoryLedger ledger) {
        return ledger;
    }

    @Bean
    public CommonService commonService(InMemoryLedger ledger, OperationStore operationStore) {
        return new OperationTrackingCommonService(ledger, operationStore);
    }

    @Bean
    public OperationStore operationStore() {
        return new InMemoryOperationStore();
    }
}
