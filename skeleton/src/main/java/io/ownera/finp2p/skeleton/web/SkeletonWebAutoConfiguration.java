package io.ownera.finp2p.skeleton.web;

import io.ownera.ledger.adapter.service.CommonService;
import io.ownera.ledger.adapter.service.EscrowService;
import io.ownera.ledger.adapter.service.HealthService;
import io.ownera.ledger.adapter.service.PaymentService;
import io.ownera.ledger.adapter.service.PlanApprovalService;
import io.ownera.ledger.adapter.service.TokenService;
import io.ownera.ledger.adapter.service.TransactionHook;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

/**
 * Capability-driven HTTP wiring for the skeleton.
 *
 * <p><strong>Principle:</strong> capability presence ↔ route presence. Each adapter-facing
 * controller is declared as a conditional {@code @Bean} below; the controller's route is only
 * registered with Spring MVC if the corresponding service bean is present in the application
 * context. An adapter that doesn't supply a {@link TokenService} doesn't expose
 * {@code /api/assets/*}; one that doesn't supply a {@link PlanApprovalService} doesn't expose
 * {@code /api/plan/*}; and so on. An empty adapter (zero services) starts cleanly and serves
 * nothing — accurately reflecting that it has no capabilities to offer.
 *
 * <p>The controller classes live under {@code io.ownera.finp2p.skeleton.web} — deliberately
 * outside any adapter's typical {@code @ComponentScan} root — so they are <em>never</em>
 * auto-detected. The auto-config below is the only registration path, which is why the
 * {@link ConditionalOnBean} checks fire reliably: when this config is processed, the user's
 * configuration has already declared (or not declared) the candidate service beans.
 *
 * <p>Registered via {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 */
@AutoConfiguration
public class SkeletonWebAutoConfiguration {

    @Bean
    @ConditionalOnBean(PlanApprovalService.class)
    public PlanApprovalController planApprovalController(PlanApprovalService planApprovalService) {
        return new PlanApprovalController(planApprovalService);
    }

    @Bean
    @ConditionalOnBean(TokenService.class)
    public TokenController tokenController(TokenService tokenService,
                                           Optional<TransactionHook> transactionHook) {
        return new TokenController(tokenService, transactionHook);
    }

    @Bean
    @ConditionalOnBean(EscrowService.class)
    public EscrowController escrowController(EscrowService escrowService,
                                             Optional<TransactionHook> transactionHook) {
        return new EscrowController(escrowService, transactionHook);
    }

    @Bean
    @ConditionalOnBean(PaymentService.class)
    public PaymentController paymentController(PaymentService paymentService,
                                               Optional<TransactionHook> transactionHook) {
        return new PaymentController(paymentService, transactionHook);
    }

    @Bean
    @ConditionalOnBean(CommonService.class)
    public ReceiptController receiptController(CommonService commonService) {
        return new ReceiptController(commonService);
    }

    @Bean
    @ConditionalOnBean(OperationStore.class)
    public OperationStatusController operationStatusController(OperationStore operationStore) {
        return new OperationStatusController(operationStore);
    }

    @Bean
    @ConditionalOnBean(HealthService.class)
    public HealthController healthController(HealthService healthService) {
        return new HealthController(healthService);
    }
}
