package io.ownera.ledger.adapter.config;

import io.ownera.finp2p.FinP2PSDK;
import io.ownera.ledger.adapter.service.proof.ProofProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProofConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ProofConfiguration.class);

    @Bean
    @ConditionalOnProperty(name = "adapter.proof.enabled", havingValue = "true")
    public ProofProvider proofProvider(
            @Value("${ORG_ID}") String orgId,
            @Value("${FINP2P_ADDRESS}") String finApiUrl,
            @Value("${OSS_URL}") String ossUrl,
            @Value("${SIGNER_PRIVATE_KEY}") String signerPrivateKey
    ) {
        logger.info("Initializing ProofProvider for org: {}", orgId);
        FinP2PSDK finP2PSDK = new FinP2PSDK(orgId, finApiUrl, ossUrl);
        return new ProofProvider(orgId, finP2PSDK, signerPrivateKey);
    }
}
