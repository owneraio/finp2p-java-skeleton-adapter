package io.ownera.ledger.adapter;

public class RegulationErrorDetails {

    public RegulationErrorDetails(String regulationType, String description) {
        this.regulationType = regulationType;
        this.description = description;
    }

    String regulationType;
    String description;
}
