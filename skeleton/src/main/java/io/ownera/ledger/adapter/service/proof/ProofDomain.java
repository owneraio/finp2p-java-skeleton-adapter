package io.ownera.ledger.adapter.service.proof;

public class ProofDomain {
    public final int chainId;
    public final String verifyingContractId;

    public ProofDomain(int chainId, String verifyingContractId) {
        this.chainId = chainId;
        this.verifyingContractId = verifyingContractId;
    }
}
