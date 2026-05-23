package io.ownera.ledger.adapter.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Zero-service adapter — proves the capability-presence ↔ route-presence principle holds
 * at the empty end of the spectrum. The application context starts cleanly without any
 * service beans, and <em>every</em> framework route returns 404 because no controller is
 * conditionally activated. No misleading "wired-but-throws" 500s or 501s.
 */
@SpringBootTest(
        classes = EmptyAdapterTest.EmptyAdapterApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        // Stub DB properties so the sample-adapter's application.properties placeholders
        // resolve. The empty-adapter test deliberately doesn't wire any data source — these
        // exist purely to satisfy property-placeholder expansion, not to connect to a DB.
        properties = {
                "DB_CONNECTION_STRING=jdbc:postgresql://invalid:1/none",
                "DB_USERNAME=none",
                "DB_PASSWORD=none",
                "spring.flyway.enabled=false"
        })
class EmptyAdapterTest {

    /** Minimal Spring Boot bootstrap that supplies no service beans. */
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
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
