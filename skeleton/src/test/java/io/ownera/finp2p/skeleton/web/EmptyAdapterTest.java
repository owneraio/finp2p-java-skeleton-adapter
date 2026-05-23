package io.ownera.finp2p.skeleton.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Zero-service adapter — proves the capability-presence ↔ route-presence principle holds at
 * the empty end of the spectrum, and that the bare framework has no DB / Flyway / jOOQ
 * runtime requirements.
 *
 * <p>The test deliberately lives in {@code skeleton/src/test} so the classpath at execution
 * time mirrors what a real limited-access adapter sees: skeleton + Spring Boot's test starter,
 * <em>no</em> PostgreSQL driver, <em>no</em> Flyway, <em>no</em> jOOQ at runtime. If
 * anything in the framework silently expected those, this test would fail to bootstrap. It
 * doesn't, because the framework doesn't.
 *
 * <p>Spring Boot starts, no service beans are wired, and every framework route returns 404
 * because no controller is conditionally activated. No misleading "wired-but-throws" 500s
 * or 501s.
 */
@SpringBootTest(
        classes = EmptyAdapterTest.EmptyAdapterApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmptyAdapterTest {

    /** Minimal Spring Boot bootstrap that supplies no service beans and no DataSource. */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    public static class EmptyAdapterApp {}

    @Autowired
    private TestRestTemplate rest;

    @Test
    void contextStartsWithNoServiceBeansWired() {
        // Boot didn't fail — the context starts even though no service beans are wired. This
        // is the "limited-access mode" baseline.
        assertNotNull(rest, "Spring Boot must have started without any service beans");
    }

    @Test
    void planApprovalRoutesAreAbsent() {
        // No PlanApprovalService bean → no PlanApprovalController → no routes.
        assertEquals(404, post("/api/plan/approve", "{\"executionPlan\":{\"id\":\"x\"}}").getStatusCodeValue());
    }

    @Test
    void tokenRoutesAreAbsent() {
        assertEquals(404, post("/api/assets/create", "{}").getStatusCodeValue());
        assertEquals(404, post("/api/assets/issue", "{}").getStatusCodeValue());
        assertEquals(404, post("/api/assets/transfer", "{}").getStatusCodeValue());
        assertEquals(404, post("/api/assets/redeem", "{}").getStatusCodeValue());
        assertEquals(404, post("/api/assets/getBalance", "{}").getStatusCodeValue());
    }

    @Test
    void escrowRoutesAreAbsent() {
        assertEquals(404, post("/api/assets/hold", "{}").getStatusCodeValue());
        assertEquals(404, post("/api/assets/release", "{}").getStatusCodeValue());
        assertEquals(404, post("/api/assets/rollback", "{}").getStatusCodeValue());
    }

    @Test
    void paymentRoutesAreAbsent() {
        assertEquals(404, post("/api/payments/depositInstruction", "{}").getStatusCodeValue());
        assertEquals(404, post("/api/payments/payout", "{}").getStatusCodeValue());
    }

    @Test
    void receiptAndOperationStatusRoutesAreAbsent() {
        // CommonService missing → /api/assets/receipts/{id} absent.
        assertEquals(404, rest.getForEntity("/api/assets/receipts/anything", String.class).getStatusCodeValue());
        // OperationStore missing → polling endpoint absent.
        assertEquals(404, rest.getForEntity("/api/operations/status/anything", String.class).getStatusCodeValue());
    }

    @Test
    void healthRoutesAreAbsent() {
        // No HealthService bean → no probes either. Adapters that need probes wire a
        // DefaultHealthService (or their own) — see the SimpleHealthService bean in the
        // normal sample-adapter for the standard pattern.
        assertEquals(404, rest.getForEntity("/health", String.class).getStatusCodeValue());
        assertEquals(404, rest.getForEntity("/health/liveness", String.class).getStatusCodeValue());
        assertEquals(404, rest.getForEntity("/health/readiness", String.class).getStatusCodeValue());
    }

    private ResponseEntity<String> post(String url, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }
}
