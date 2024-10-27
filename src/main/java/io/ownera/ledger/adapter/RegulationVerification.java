package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.graphql.GraphqlClient;
import io.ownera.ledger.adapter.graphql.ItemNotFoundException;
import io.ownera.ledger.adapter.graphql.models.AssetDetails;
import io.ownera.ledger.adapter.graphql.models.CertificateNode;
import io.ownera.ledger.adapter.graphql.models.RegulationVerifier;
import io.ownera.ledger.adapter.graphql.models.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegulationVerification {
    private final GraphqlClient client;

    private final static Logger logger = LoggerFactory.getLogger(RegulationVerification.class);

    public RegulationVerification(String ossUrl) {
        client = new GraphqlClient(ossUrl);
    }

    public RegulationVerification(GraphqlClient client) {
        this.client = client;
    }

    public List<RegulationErrorDetails> doRegulationCheck(String finId, String assetId) throws IOException, ItemNotFoundException {
        logger.debug("Regulation check for user {} / asset {}", finId, assetId);
        UserDetails user = client.getUserDetails(finId);
        AssetDetails asset = client.getAssetDetails(assetId);

        List<RegulationErrorDetails> regErrs = new ArrayList<>();
        for (RegulationVerifier verifier : asset.getRegulationVerifiers()) {
            if (!verifier.getName().isEmpty()) {
                boolean found = false;
                for (CertificateNode cert : user.getCertificates().getNodes()) {
                    if (verifier.getName().equals(cert.getType())) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    logger.debug("user doesn't meet '{}' regulation requirement", verifier.getName());
                    String message = String.format("user doesn't meet '%s' regulation requirement", verifier.getName());
                    regErrs.add(new RegulationErrorDetails(verifier.getName(), message));
                }
            }
        }
        return regErrs;
    }
}
