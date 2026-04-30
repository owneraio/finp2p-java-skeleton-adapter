package io.ownera.ledger.adapter.config;

import io.ownera.ledger.adapter.sample.db.DbLedger;
import io.ownera.ledger.adapter.service.workflow.DbOperationStore;
import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.service.asset.AssetStore;
import io.ownera.ledger.adapter.service.asset.DbAssetStore;
import io.ownera.ledger.adapter.service.mapping.DbAccountMappingStore;
import io.ownera.ledger.adapter.service.proof.ProofProvider;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
import io.ownera.ledger.adapter.service.workflow.OperationTrackingCommonService;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Optional;

@Configuration
public class PostgresConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(PostgresConfiguration.class);

    /**
     * Schema name for skeleton-owned tables. Driven by {@code LEDGER_SCHEMA} (operator override)
     * with this adapter's hardcoded default {@code sample_adapter}. Real adapters substitute
     * their own default (e.g. {@code hedera}, {@code canton}) so multiple adapter types coexist
     * on a shared DB without colliding.
     */
    @Value("${ledger.schema:sample_adapter}")
    private String ledgerSchema;

    @Bean
    public DSLContext dslContext(DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }

    @Bean
    public DbLedger dbLedger(DSLContext dslContext, AssetStore assetStore, Optional<ProofProvider> proofProvider) {
        logger.info("Initializing DbLedger with schema={}", ledgerSchema);
        return new DbLedger(dslContext, ledgerSchema, assetStore, proofProvider.orElse(null));
    }

    @Bean
    public TokenService tokenService(DbLedger ledger) {
        return ledger;
    }

    @Bean
    public EscrowService escrowService(DbLedger ledger) {
        return ledger;
    }

    @Bean
    public CommonService commonService(DbLedger ledger, OperationStore operationStore) {
        return new OperationTrackingCommonService(ledger, operationStore);
    }

    @Bean
    public OperationStore operationStore(DSLContext dslContext) {
        return new DbOperationStore(dslContext, ledgerSchema);
    }

    @Bean
    public DbAccountMappingStore accountMappingStore(DSLContext dslContext) {
        return new DbAccountMappingStore(dslContext, ledgerSchema);
    }

    @Bean
    public AssetStore assetStore(DSLContext dslContext) {
        return new DbAssetStore(dslContext, ledgerSchema);
    }
}
