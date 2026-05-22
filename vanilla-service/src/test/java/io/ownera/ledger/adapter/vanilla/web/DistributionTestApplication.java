package io.ownera.ledger.adapter.vanilla.web;

import io.ownera.ledger.adapter.vanilla.DistributionRoutesConfig;
import io.ownera.ledger.adapter.vanilla.DistributionService;
import io.ownera.ledger.adapter.vanilla.LedgerStorage;
import io.ownera.ledger.adapter.vanilla.OmnibusDelegate;
import io.ownera.ledger.adapter.vanilla.VanillaDistributionService;
import io.ownera.ledger.adapter.vanilla.VanillaPostgresHolder;
import org.jooq.DSLContext;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Minimal Spring Boot bootstrap for {@link DistributionControllerTest}.
 *
 * <p>Vanilla-service is a library; this class only exists so the test can run the
 * {@code /distribution/*} routes behind a real HTTP server. It plays the role of an
 * adapter: provides a {@link DistributionService} bean (built from the shared
 * {@link LedgerStorage} + a mocked {@link OmnibusDelegate}) and opts into the routes with
 * {@code @Import(DistributionRoutesConfig.class)} — exactly what a real adapter would do.
 *
 * <p>The bootstrap deliberately lives in {@code …vanilla.web} (a sub-package of vanilla's
 * production root). {@code @SpringBootApplication} default component-scan starts at this
 * class's package and goes <em>down</em>, so {@code DistributionController} at
 * {@code …vanilla} is <strong>not</strong> auto-scanned — the only path that registers the
 * controller bean is the {@code @Import(DistributionRoutesConfig.class)} below, mirroring
 * what a real adapter does.
 *
 * <p>DataSource auto-configuration is excluded because we provide the jOOQ
 * {@link DSLContext} directly from the test container.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@Import(DistributionRoutesConfig.class)
public class DistributionTestApplication {

    @Bean
    public DSLContext dslContext() {
        return VanillaPostgresHolder.CTX;
    }

    @Bean
    public LedgerStorage ledgerStorage(DSLContext dsl) {
        return new LedgerStorage(dsl, VanillaPostgresHolder.SCHEMA);
    }

    @Bean
    public OmnibusDelegate omnibusDelegate() {
        return Mockito.mock(OmnibusDelegate.class);
    }

    @Bean
    public DistributionService distributionService(LedgerStorage storage, OmnibusDelegate delegate) {
        return new VanillaDistributionService(storage, delegate);
    }

    public static void main(String[] args) {
        SpringApplication.run(DistributionTestApplication.class, args);
    }
}
