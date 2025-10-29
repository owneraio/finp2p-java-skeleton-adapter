package io.ownera.ledger.adapter.service.model;

public class AdditionalContractDetails {
    public final String finP2POperatorContractAddress;
    public final boolean allowanceRequired;

    public AdditionalContractDetails(String finP2POperatorContractAddress, boolean allowanceRequired) {
        this.finP2POperatorContractAddress = finP2POperatorContractAddress;
        this.allowanceRequired = allowanceRequired;
    }
}
