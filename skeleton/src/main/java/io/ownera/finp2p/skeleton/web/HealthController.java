package io.ownera.finp2p.skeleton.web;

import io.ownera.ledger.adapter.service.HealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /health}, {@code /health/liveness}, {@code /health/readiness} probes — only loaded
 * when a {@link HealthService} bean exists.
 *
 * <p>Adapters that don't want to participate in health checking omit the bean; in that case
 * the platform (k8s probe, load balancer, etc.) needs to be configured to not look for these
 * endpoints, or to interpret a 404 as "process up." Adapters that want the standard probes
 * can wire any {@link HealthService} implementation — including the no-op
 * {@link io.ownera.ledger.adapter.service.DefaultHealthService} shipped with the skeleton.
 */
@RestController
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping(value = "/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping(value = "/health/liveness")
    public ResponseEntity<String> liveness(
            @RequestHeader(name = "skip-vendor", required = false) String skipVendor) {
        if (!"true".equals(skipVendor)) {
            healthService.liveness();
        }
        return ResponseEntity.ok("OK");
    }

    @GetMapping(value = "/health/readiness")
    public ResponseEntity<String> readiness(
            @RequestHeader(name = "skip-vendor", required = false) String skipVendor) {
        if (!"true".equals(skipVendor)) {
            healthService.readiness();
        }
        return ResponseEntity.ok("OK");
    }
}
