package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.EIP712Template;
import io.ownera.ledger.adapter.service.model.HashListTemplate;
import io.ownera.ledger.adapter.service.model.Signature;
import io.ownera.finp2p.signing.SignatureUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SignatureException;

public class SignatureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(SignatureVerifier.class);

    public boolean verify(Signature signature, String signerFinId) {
        if (signature == null) {
            logger.warn("No signature provided");
            return false;
        }

        try {
            if (signature.template instanceof EIP712Template) {
                return verifyEIP712(signature, signerFinId);
            } else if (signature.template instanceof HashListTemplate) {
                return verifyHashList(signature, signerFinId);
            } else {
                logger.warn("Unsupported signature template type: {}", signature.template.getClass().getName());
                return false;
            }
        } catch (Exception e) {
            logger.warn("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean verifyEIP712(Signature signature, String signerFinId) {
        EIP712Template template = (EIP712Template) signature.template;

        byte[] hashBytes = Hex.decode(template.hash);
        byte[] signatureBytes = Hex.decode(signature.signature);

        try {
            SignatureUtils.verify(signerFinId, signatureBytes, hashBytes);
            return true;
        } catch (SignatureException e) {
            logger.warn("EIP712 signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean verifyHashList(Signature signature, String signerFinId) {
        HashListTemplate template = (HashListTemplate) signature.template;

        byte[] hashBytes = Hex.decode(template.hash);
        byte[] signatureBytes = Hex.decode(signature.signature);

        try {
            SignatureUtils.verify(signerFinId, signatureBytes, hashBytes);
            return true;
        } catch (SignatureException e) {
            logger.warn("HashList signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
