package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.sample.*;
import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.service.proof.ProofProvider;
import io.ownera.ledger.adapter.service.workflow.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@SpringBootApplication
@Configuration
public class Adapter {

    private final static Logger logger = LoggerFactory.getLogger(Adapter.class);

    @Value("${ORG_ID}")
    private String orgId;

    @Bean
    public InMemoryLedger inMemoryLedger(Optional<ProofProvider> proofProvider) {
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
    public PaymentService paymentService() {
        return new CollateralService();
    }

    @Bean
    public PlanApprovalService planApprovalService() {
        return new AutoPlanApprovalService();
    }

    @Bean
    public CommonService commonService(InMemoryLedger ledger, OperationStore operationStore) {
        return new OperationTrackingCommonService(ledger, operationStore);
    }

    @Bean
    public SimpleHealthService healthService() {
        return new SimpleHealthService();
    }

    @Bean
    public SignatureVerifier signatureVerifier() {
        return new SignatureVerifier();
    }

    @Bean
    public CryptoService cryptoService() {
        return new CryptoService();
    }

    @Bean
    public OperationStore operationStore() {
        return new InMemoryOperationStore();
    }

    @Bean
    public OperationExecutor operationExecutor(OperationStore operationStore) {
        return new OperationExecutor(operationStore, null);
    }

    // Uncomment to enable transaction lifecycle hooks:
    // @Bean
    // public TransactionHook transactionHook() {
    //     return new LoggingTransactionHook();
    // }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> onReady(SimpleHealthService healthService) {
        return event -> {
            logger.info("Application ready, marking health service as initialized");
            healthService.markReady();
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(Adapter.class, args);
    }
}
