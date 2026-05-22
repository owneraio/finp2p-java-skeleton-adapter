package io.ownera.ledger.adapter.vanilla;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Opt-in entry point for the distribution HTTP routes.
 *
 * <p>Mirrors Node's {@code registerDistributionRoutes(app, distributionService)} callback
 * ({@code vanilla-service/src/routes.ts}). In Spring the equivalent of "register these
 * routes on my app" is "import this config class": adapters wire the routes by adding
 *
 * <pre>{@code
 *   @Import(DistributionRoutesConfig.class)
 * }</pre>
 *
 * to their own Spring configuration. The adapter is responsible for supplying a
 * {@link DistributionService} bean — typically a {@link VanillaDistributionService}
 * constructed with a {@link LedgerStorage} and an {@link OmnibusDelegate} the adapter
 * implements.
 *
 * <p>Vanilla-service stays a library: the controller class is present in the JAR but
 * inert unless the adapter explicitly imports this config.
 */
@Configuration
public class DistributionRoutesConfig {

    @Bean
    public DistributionController distributionController(DistributionService distributionService) {
        return new DistributionController(distributionService);
    }
}
