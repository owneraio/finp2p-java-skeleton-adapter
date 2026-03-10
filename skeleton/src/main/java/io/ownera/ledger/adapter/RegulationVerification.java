package io.ownera.ledger.adapter;

import io.ownera.finp2p.FinP2PSDK;
import io.ownera.finp2p.oss.GraphqlException;
import io.ownera.finp2p.oss.models.Certificate;
import io.ownera.finp2p.oss.models.OSSRegulationVerifier;
import io.ownera.finp2p.oss.models.OssAsset;
import io.ownera.finp2p.oss.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RegulationVerification {
    private final FinP2PSDK finp2p;

    private final static Logger logger = LoggerFactory.getLogger(RegulationVerification.class);

    public RegulationVerification(String orgId, String finAPIUrl, String ossUrl) {
        finp2p = new FinP2PSDK(orgId, finAPIUrl, ossUrl);
    }


    public List<RegulationErrorDetails> doRegulationCheck(String finId, String assetId) throws IOException, GraphqlException {
        logger.debug("Regulation check for user {} / asset {}", finId, assetId);
        Optional<User> user = finp2p.getUserByFinId(finId);
        if (user.isEmpty()) {
            throw new GraphqlException("User with finId " + finId + " not found");
        }
        Optional<OssAsset> asset = finp2p.getAsset(assetId);
        if (asset.isEmpty()) {
            throw new GraphqlException("Asset with id " + assetId + " not found");
        }

        List<RegulationErrorDetails> regErrs = new ArrayList<>();
        for (OSSRegulationVerifier verifier : asset.get().getRegulationVerifiers()) {
            if (!verifier.getName().isEmpty()) {
                boolean found = false;
                for (Certificate cert : user.get().getCertificates().getNodes()) {
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
