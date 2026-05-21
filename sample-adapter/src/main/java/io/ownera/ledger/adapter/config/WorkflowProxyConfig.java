package io.ownera.ledger.adapter.config;

import io.ownera.ledger.adapter.sample.AutoPlanApprovalService;
import io.ownera.ledger.adapter.sample.CollateralService;
import io.ownera.ledger.adapter.sample.db.DbLedger;
import io.ownera.ledger.adapter.service.EscrowService;
import io.ownera.ledger.adapter.service.PaymentService;
import io.ownera.ledger.adapter.service.PlanApprovalService;
import io.ownera.ledger.adapter.service.TokenService;
import io.ownera.ledger.adapter.service.workflow.CallbackClient;
import io.ownera.ledger.adapter.service.workflow.OperationOutputSerializer;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
import io.ownera.ledger.adapter.service.workflow.WorkflowArgsCodec;
import io.ownera.ledger.adapter.service.workflow.WorkflowRecovery;
import io.ownera.ledger.adapter.service.workflow.WorkflowServiceProxy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wires the durable async workflow path: each of TokenService / EscrowService / PaymentService /
 * PlanApprovalService is exposed as a {@link WorkflowServiceProxy}-wrapped bean, and a single
 * {@link ApplicationReadyEvent} listener replays any {@code IN_PROGRESS} operations left by a
 * prior crash.
 *
 * <p>Mirrors the Node skeleton's {@code createServiceProxy} pattern: the proxied service methods
 * persist a pending payload, run the real method in the background, and finalize (persist outputs
 * + send callback). Pollers see the cid through {@code GET /api/operations/status/{cid}}.
 *
 * <p>Sample adapter chooses no {@link CallbackClient} (router-side polling). Real adapters can
 * register one as a bean and the proxy will pick it up automatically.
 */
@Configuration
public class WorkflowProxyConfig {

    // Method-name sets per service. Non-listed methods (e.g. balance reads, proposalStatus
    // notifications) pass through the proxy unchanged. Match the Node skeleton's wrappedResponse
    // switch in skeleton/src/workflows/service.ts.
    private static final Set<String> TOKEN_METHODS =
            Set.of("createAsset", "issue", "transfer", "redeem");
    private static final Set<String> ESCROW_METHODS =
            Set.of("hold", "release", "rollback");
    private static final Set<String> PAYMENT_METHODS =
            Set.of("getDepositInstruction", "payout");
    private static final Set<String> PLAN_METHODS =
            Set.of("approvePlan", "proposeCancelPlan", "proposeResetPlan", "proposeInstructionApproval");

    /** Background pool for the proxies; cached so idle threads exit during quiet periods. */
    @Bean
    public ExecutorService workflowExecutor() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public WorkflowArgsCodec workflowArgsCodec() {
        return new WorkflowArgsCodec();
    }

    private WorkflowServiceProxy newHandler(Object target,
                                            OperationStore store,
                                            Optional<CallbackClient> callback,
                                            ExecutorService executor,
                                            WorkflowArgsCodec codec,
                                            Set<String> methods) {
        return new WorkflowServiceProxy(target, store, callback.orElse(null), executor, codec,
                OperationOutputSerializer.defaultSerializer(), methods);
    }

    @Bean
    @Primary
    public TokenService tokenService(DbLedger ledger,
                                     OperationStore store,
                                     Optional<CallbackClient> callback,
                                     ExecutorService workflowExecutor,
                                     WorkflowArgsCodec codec,
                                     List<ProxyEntry> registry) {
        WorkflowServiceProxy handler = newHandler(ledger, store, callback, workflowExecutor, codec, TOKEN_METHODS);
        registry.add(new ProxyEntry(handler, TOKEN_METHODS));
        return handler.asProxy(TokenService.class);
    }

    @Bean
    @Primary
    public EscrowService escrowService(DbLedger ledger,
                                       OperationStore store,
                                       Optional<CallbackClient> callback,
                                       ExecutorService workflowExecutor,
                                       WorkflowArgsCodec codec,
                                       List<ProxyEntry> registry) {
        WorkflowServiceProxy handler = newHandler(ledger, store, callback, workflowExecutor, codec, ESCROW_METHODS);
        registry.add(new ProxyEntry(handler, ESCROW_METHODS));
        return handler.asProxy(EscrowService.class);
    }

    @Bean
    @Primary
    public PaymentService paymentService(OperationStore store,
                                         Optional<CallbackClient> callback,
                                         ExecutorService workflowExecutor,
                                         WorkflowArgsCodec codec,
                                         List<ProxyEntry> registry) {
        CollateralService raw = new CollateralService();
        WorkflowServiceProxy handler = newHandler(raw, store, callback, workflowExecutor, codec, PAYMENT_METHODS);
        registry.add(new ProxyEntry(handler, PAYMENT_METHODS));
        return handler.asProxy(PaymentService.class);
    }

    @Bean
    @Primary
    public PlanApprovalService planApprovalService(OperationStore store,
                                                   Optional<CallbackClient> callback,
                                                   ExecutorService workflowExecutor,
                                                   WorkflowArgsCodec codec,
                                                   List<ProxyEntry> registry) {
        AutoPlanApprovalService raw = new AutoPlanApprovalService();
        WorkflowServiceProxy handler = newHandler(raw, store, callback, workflowExecutor, codec, PLAN_METHODS);
        registry.add(new ProxyEntry(handler, PLAN_METHODS));
        return handler.asProxy(PlanApprovalService.class);
    }

    @Bean
    public List<ProxyEntry> workflowProxyRegistry() {
        return new ArrayList<>();
    }

    /**
     * Run crash recovery once Spring has fully started: for each proxied service, fetch any
     * IN_PROGRESS rows and re-drive them through the proxy. Idempotent — replayed work either
     * completes successfully (and now matches the row's persisted outputs) or fails and writes a
     * failure payload.
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> workflowRecoveryHook(
            OperationStore store,
            WorkflowArgsCodec codec,
            List<ProxyEntry> registry) {
        return event -> {
            for (ProxyEntry entry : registry) {
                WorkflowRecovery recovery = new WorkflowRecovery(
                        store, entry.proxy, List.copyOf(entry.methods), codec);
                recovery.replayAll();
            }
        };
    }

    public static final class ProxyEntry {
        final WorkflowServiceProxy proxy;
        final Set<String> methods;
        ProxyEntry(WorkflowServiceProxy proxy, Set<String> methods) {
            this.proxy = proxy;
            this.methods = methods;
        }
    }
}
