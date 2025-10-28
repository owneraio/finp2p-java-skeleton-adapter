package io.ownera.ledger.adapter.service.proof;

public class SignatureAssetProofPolicy implements AssetProofPolicy {
    public final String verifyingKey;
    public final String signatureTemplate;
    public final ProofDomain domain;

    public SignatureAssetProofPolicy(String verifyingKey, String signatureTemplate, ProofDomain domain) {
        this.verifyingKey = verifyingKey;
        this.signatureTemplate = signatureTemplate;
        this.domain = domain;
    }
}
