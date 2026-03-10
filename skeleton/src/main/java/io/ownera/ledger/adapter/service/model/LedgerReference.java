package io.ownera.ledger.adapter.service.model;

public class LedgerReference {
    public final String network;
    public final String address;
    public final String tokenStandard;
    public final AdditionalContractDetails additionalContractDetails;

    public LedgerReference(String network, String address, String tokenStandard, AdditionalContractDetails additionalContractDetails) {
        this.network = network;
        this.address = address;
        this.tokenStandard = tokenStandard;
        this.additionalContractDetails = additionalContractDetails;
    }
}
