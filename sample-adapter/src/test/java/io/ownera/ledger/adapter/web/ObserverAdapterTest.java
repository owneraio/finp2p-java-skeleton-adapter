package io.ownera.ledger.adapter.web;

import io.ownera.ledger.adapter.service.PlanApprovalService;
import io.ownera.ledger.adapter.service.model.ApprovedPlan;
import io.ownera.ledger.adapter.service.model.PlanApprovalStatus;
import io.ownera.ledger.adapter.service.model.PlanProposal;
import io.ownera.ledger.adapter.service.model.ProposalStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Observer-mode adapter — wires <em>only</em> a {@link PlanApprovalService} (the limited-access
 * deployment shape that motivated the conditional-controller split). The {@code /api/plan/*}
 * routes activate; everything else stays absent (404).
 */
@SpringBootTest(
        classes = ObserverAdapterTest.ObserverAdapterApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "DB_CONNECTION_STRING=jdbc:postgresql://invalid:1/none",
                "DB_USERNAME=none",
                "DB_PASSWORD=none",
                "spring.flyway.enabled=false"
        })
class ObserverAdapterTest {

    /** Trivial PlanApprovalService that always approves — enough to prove the route is alive. */
    static class AlwaysApprove implements PlanApprovalService {
        @Override public PlanApprovalStatus approvePlan(String idempotencyKey, String planId) { return new ApprovedPlan(); }
        @Override public PlanApprovalStatus proposeCancelPlan(String idempotencyKey, String planId) { return new ApprovedPlan(); }
        @Override public PlanApprovalStatus proposeResetPlan(String idempotencyKey, String planId, int proposedSequence) { return new ApprovedPlan(); }
        @Override public PlanApprovalStatus proposeInstructionApproval(String idempotencyKey, String planId, int instructionSequence) { return new ApprovedPlan(); }
        @Override public void proposalStatus(String planId, PlanProposal proposal, ProposalStatus status) {}
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    public static class ObserverAdapterApp {
        @Bean
        public PlanApprovalService planApprovalService() {
            return new AlwaysApprove();
        }
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    void planApprovalRouteIsActiveWhenPlanApprovalServiceIsWired() {
        // POST /api/plan/approve with a minimal body. The route exists → 200 (or at worst a 400
        // for body validation), NOT a 404.
        String body = "{\"executionPlan\":{\"id\":\"plan-observer\"}}";
        ResponseEntity<String> resp = post("/api/plan/approve", body);
        assertEquals(200, resp.getStatusCodeValue(),
                "PlanApprovalController must activate when PlanApprovalService is wired");
    }

    @Test
    void tokenEscrowPaymentRoutesAreAbsent() {
        // No TokenService / EscrowService / PaymentService → no routes. 404.
        assertEquals(404, post("/api/assets/create", "{}").getStatusCodeValue());
        assertEquals(404, post("/api/assets/issue", "{}").getStatusCodeValue());
        assertEquals(404, post("/api/assets/hold", "{}").getStatusCodeValue());
        assertEquals(404, post("/api/payments/payout", "{}").getStatusCodeValue());
    }

    @Test
    void receiptAndOperationStatusRoutesAreAbsent() {
        // CommonService / OperationStore missing → corresponding routes absent.
        assertEquals(404, rest.getForEntity("/api/assets/receipts/x", String.class).getStatusCodeValue());
        assertEquals(404, rest.getForEntity("/api/operations/status/x", String.class).getStatusCodeValue());
    }

    @Test
    void healthRoutesAreAbsentBecauseNoHealthServiceBeanIsWired() {
        // Observer-mode here doesn't bother with health probes. Adapters that need them wire a
        // HealthService (e.g. the shipped DefaultHealthService).
        assertEquals(404, rest.getForEntity("/health", String.class).getStatusCodeValue());
    }

    private ResponseEntity<String> post(String url, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }
}
