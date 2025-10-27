package io.ownera.ledger.adapter.service.model;

import io.ownera.finp2p.common.HashAlgorithm;
import io.ownera.finp2p.common.SignatureTemplate;

public class SignatureProofPolicy implements ProofPolicy {
    public final HashAlgorithm hashAlgorithm;
    public final SignatureTemplate signatureTemplate;
    public final String signature;

    public SignatureProofPolicy(HashAlgorithm hashAlgorithm, SignatureTemplate signatureTemplate, String signature) {
        this.hashAlgorithm = hashAlgorithm;
        this.signatureTemplate = signatureTemplate;
        this.signature = signature;
    }
}
