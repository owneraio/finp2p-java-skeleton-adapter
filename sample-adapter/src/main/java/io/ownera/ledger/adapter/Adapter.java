package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.sample.*;
import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.service.workflow.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
public class Adapter {

    private final static Logger logger = LoggerFactory.getLogger(Adapter.class);

    @Bean
    public PaymentService paymentService() {
        return new CollateralService();
    }

    @Bean
    public PlanApprovalService planApprovalService() {
        return new AutoPlanApprovalService();
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
