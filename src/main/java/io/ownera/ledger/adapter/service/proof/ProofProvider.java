package io.ownera.ledger.adapter.service.proof;

import io.ownera.finp2p.FinP2PSDK;
import io.ownera.finp2p.oss.GraphqlException;
import io.ownera.finp2p.oss.models.OssAsset;
import io.ownera.finp2p.oss.models.PaymentAsset;
import io.ownera.finp2p.oss.models.Proof;
import io.ownera.finp2p.signing.SignatureUtils;
import io.ownera.finp2p.signing.eip712.EIP712;
import io.ownera.finp2p.signing.eip712.models.Domain;
import io.ownera.ledger.adapter.service.model.*;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static io.ownera.finp2p.signing.SignatureUtils.sign;
import static io.ownera.finp2p.signing.SignatureUtils.toFinId;
import static io.ownera.finp2p.signing.eip712.EIP712.hashMessage;
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
        Proof policy;
        try {
            policy = getAssetProofPolicy(a.assetId, a.assetType, orgId);
        } catch (GraphqlException e) {
            throw new ProofProviderException("Failed to retrieve asset proof policy" + e.getMessage());
        }
        if (policy instanceof io.ownera.finp2p.oss.models.NoProofPolicy) {
            receipt.proof = new NoProofPolicy();
        } else if (policy instanceof io.ownera.finp2p.oss.models.SignatureProofPolicy) {
            io.ownera.finp2p.oss.models.SignatureProofPolicy proofPolicy = ((io.ownera.finp2p.oss.models.SignatureProofPolicy) policy);
            if (!proofPolicy.getSignatureTemplate().equals("EIP712")) {
                throw new ProofProviderException("Unsupported signature template: " + proofPolicy.getSignatureTemplate());
            }
            AsymmetricCipherKeyPair signerKeyPair;
            try {
                signerKeyPair = SignatureUtils.parseKey(signerPrivateKey);
            } catch (IOException e) {
                throw new ProofProviderException("Failed to parse signer private key");
            }
            ECPrivateKeyParameters signerPrivateKey = (ECPrivateKeyParameters) signerKeyPair.getPrivate();
            ECPublicKeyParameters signerPublicKey = (ECPublicKeyParameters) signerKeyPair.getPublic();
            String signerFinId = toFinId(signerPublicKey);
            if (!proofPolicy.getVerifyingKey().equalsIgnoreCase(signerFinId)) {
                throw new ProofProviderException("Signer public key does not match verifying key from policy");
            }
            io.ownera.finp2p.signing.eip712.models.Receipt rcp = toEIP712(receipt);
            byte[] hash;
            try {
                hash = hashMessage(rcp);
            } catch (IOException e) {
                throw new ProofProviderException("Failed to hash EIP712 message: " + e.getMessage());
            }
            String signature = Hex.encodeHexString(sign(signerPrivateKey, hash));
            EIP712Template template = new EIP712Template(
                    rcp.getTypeName(),
                    Domain.defaultDomain(),
                    rcp,
                    rcp.getTypes(),
                    Hex.encodeHexString(hash)
            );
            receipt.proof = new SignatureProofPolicy(
                    HashFunction.KECCAK_256, template, signature
            );
        }
    }


    private Proof getAssetProofPolicy(String assetId, AssetType assetType, String paymentOrgId) throws GraphqlException {
        switch (assetType) {
            case FINP2P:
                Optional<OssAsset> asset = finP2PSDK.getAsset(assetId);
                if (asset.isEmpty()) {
                    throw new ProofProviderException("Asset " + assetId + " not found");
                }
                if (asset.get().getPolicies() == null || asset.get().getPolicies().getProof() == null) {
                    throw new ProofProviderException("No proof policy found for asset " + assetId);
                }
                return asset.get().getPolicies().getProof();
            case FIAT:
            case CRYPTOCURRENCY:
//                List<PaymentAsset> paymentAssets = finP2PSDK.getPaymentAssets();
//                paymentAssets.stream()
//                        .filter(p -> p.getOrgId().equals(paymentOrgId))
//                        .filter(p -> Arrays.stream(p.getAssets()).filter(a -> a))
//                ;
              throw new ProofProviderException("Not implemented for asset type: " + assetType);
            default:
                throw new ProofProviderException("Unsupported asset type: " + assetType);
        }

    }


}
