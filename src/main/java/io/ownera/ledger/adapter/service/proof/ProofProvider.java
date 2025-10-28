package io.ownera.ledger.adapter.service.proof;

import io.ownera.finp2p.FinP2PSDK;
import io.ownera.finp2p.oss.GraphqlException;
import io.ownera.finp2p.oss.models.OssAsset;
import io.ownera.finp2p.signing.SignatureUtils;
import io.ownera.ledger.adapter.service.model.*;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

import static io.ownera.finp2p.signing.SignatureUtils.sign;
import static io.ownera.finp2p.signing.SignatureUtils.toFinId;
import static io.ownera.ledger.adapter.service.proof.Mapper.toEIP712;

public class ProofProvider {

    private static final Logger logger = LoggerFactory.getLogger(ProofProvider.class);

    private final String orgId;
    private final FinP2PSDK finP2PSDK;
    private final String signerPrivateKey;

    public ProofProvider(String orgId, FinP2PSDK finP2PSDK, String signerPrivateKey) {
        this.orgId = orgId;
        this.finP2PSDK = finP2PSDK;
        this.signerPrivateKey = signerPrivateKey;
    }

    public void provideLedgerProof(Receipt receipt) throws ProofProviderException {
        if (this.finP2PSDK == null) {
            return;
        }
        Asset a = receipt.asset;
        AssetProofPolicy policy = null;
        try {
            policy = getAssetProofPolicy(a.assetId, a.assetType, orgId);
        } catch (GraphqlException e) {
            throw new ProofProviderException("Failed to retrieve asset proof policy", e);
        }
        if (policy instanceof NoAssetProofPolicy) {
            receipt.proof = new NoProofPolicy();
        } else if (policy instanceof SignatureAssetProofPolicy) {
            SignatureAssetProofPolicy proofPolicy = ((SignatureAssetProofPolicy) policy);
            if (!proofPolicy.signatureTemplate.equals("EIP712")) {
                throw new ProofProviderException("Unsupported signature template: " + proofPolicy.signatureTemplate);
            }
            AsymmetricCipherKeyPair signerKeyPair;
            try {
                signerKeyPair = SignatureUtils.parseKey(signerPrivateKey);
            } catch (IOException e) {
                throw new ProofProviderException("Failed to parse signer private key", e);
            }
            ECPrivateKeyParameters signerPrivateKey = (ECPrivateKeyParameters) signerKeyPair.getPrivate();
            ECPublicKeyParameters signerPublicKey = (ECPublicKeyParameters) signerKeyPair.getPublic();
            String signerFinId = toFinId(signerPublicKey);
            if (!proofPolicy.verifyingKey.equalsIgnoreCase(signerFinId)) {
                throw new ProofProviderException("Signer public key does not match verifying key from policy");
            }
            byte[] hash = hashMessage(toEIP712(receipt));
            String signature = Hex.encodeHexString(sign(signerPrivateKey, hash));
            SignatureTemplate template = new EIP712Template();
            receipt.proof = new SignatureProofPolicy(
                    HashFunction.KECCAK_256, template, signature
            );
        }
    }


    private AssetProofPolicy getAssetProofPolicy(String assetId, AssetType assetType, String paymentOrgId) throws GraphqlException {
        switch (assetType) {
            case FINP2P:
                Optional<OssAsset> asset = finP2PSDK.getAsset(assetId);
                if (asset.isEmpty()) {
                    throw new ProofProviderException("Asset " + assetId + " not found");
                }
                asset.get().getProofPolicy();
                break;
            case FIAT:
            case CRYPTOCURRENCY:
                Optional<OSSPayemntAsset> paymentAsset = finP2PSDK.getPaymentAsset(assetId);
        }

    }


}
