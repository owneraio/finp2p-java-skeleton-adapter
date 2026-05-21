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

    // PaymentService and PlanApprovalService beans live in WorkflowProxyConfig — they are
    // exposed as WorkflowServiceProxy-wrapped versions so all proxied payment/plan operations
    // flow through the durable async workflow path.

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

    // OperationExecutor (the sync workflow path) is still available in the skeleton for adapters
    // that prefer it over the proxy. The sample-adapter wires services through
    // WorkflowProxyConfig instead and no longer needs this bean.

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
