package io.ownera.ledger.adapter.service;

/**
 * No-op {@link HealthService}. Useful as a one-line wiring for adapters that want the
 * standard probe endpoints exposed but don't need custom liveness/readiness logic. Adapters
 * pick this up with a single bean declaration:
 *
 * <pre>{@code
 *   @Bean
 *   public HealthService healthService() {
 *       return new DefaultHealthService();
 *   }
 * }</pre>
 *
 * <p>Adapters that need real checks (e.g. a "are migrations applied" gate) implement their
 * own {@link HealthService} instead.
 */
public class DefaultHealthService implements HealthService {

    @Override
    public void liveness() {
        // no-op
    }

    @Override
    public void readiness() {
        // no-op
    }
}
