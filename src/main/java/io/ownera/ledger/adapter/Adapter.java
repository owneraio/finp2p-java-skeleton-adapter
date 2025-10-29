package io.ownera.ledger.adapter;

import io.ownera.finp2p.FinP2PSDK;
import io.ownera.ledger.adapter.sample.AutoPlanApprovalService;
import io.ownera.ledger.adapter.sample.CollateralService;
import io.ownera.ledger.adapter.sample.InMemoryLedger;
import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.service.proof.ProofProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
public class Adapter {

    private final static Logger logger = LoggerFactory.getLogger(Adapter.class);

    @Value("${ORG_ID}")
    private String orgId;

    @Value("${SIGNER_PRIVATE_KEY}")
    private String signerPrivateKey;

    @Value("${FINP2P_ADDRESS}")
    private String finApiUrl;

    @Value("${OSS_URL}")
    private String ossUrl;

    @Bean
    public InMemoryLedger inMemoryLedger() {
        logger.info("Initializing TokenService with ORG_ID: {}", orgId);

        FinP2PSDK finP2PSDK = new FinP2PSDK(orgId , finApiUrl, ossUrl);
        ProofProvider proofProvider = new ProofProvider(orgId, finP2PSDK, signerPrivateKey);
        return new InMemoryLedger(proofProvider);
    }

    @Bean
    public TokenService tokenService(InMemoryLedger ledger) {
        return ledger;
    }

    @Bean
    public EscrowService escrowService(InMemoryLedger ledger) {
        return ledger;
    }

    @Bean
    public PaymentService paymentService(InMemoryLedger ledger) {
        return new CollateralService();
    }

    @Bean
    public PlanApprovalService planApprovalService() {
        return new AutoPlanApprovalService();
    }

    @Bean
    public CommonService commonService(InMemoryLedger ledger) {
        return ledger;
    }

    public static void main(String[] args) {
        SpringApplication.run(Adapter.class, args);
    }
}
