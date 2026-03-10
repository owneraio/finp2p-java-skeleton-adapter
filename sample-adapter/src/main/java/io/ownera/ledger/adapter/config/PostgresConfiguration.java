package io.ownera.ledger.adapter.config;

import io.ownera.ledger.adapter.sample.db.DbLedger;
import io.ownera.ledger.adapter.sample.db.DbOperationStore;
import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.service.proof.ProofProvider;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
import io.ownera.ledger.adapter.service.workflow.OperationTrackingCommonService;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.Optional;

@Configuration
@Profile("postgres")
public class PostgresConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(PostgresConfiguration.class);

    @Bean
    public DSLContext dslContext(DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }

    @Bean
    public DbLedger dbLedger(DSLContext dslContext, Optional<ProofProvider> proofProvider) {
        logger.info("Initializing DbLedger with PostgreSQL backend (jOOQ)");
        return new DbLedger(dslContext, proofProvider.orElse(null));
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
        return new DbOperationStore(dslContext);
    }
}
