package io.ownera.ledger.adapter.sample.collateral;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Collateral {
    @JsonProperty("asset")
    private CollateralAsset asset;

    @JsonProperty("custody")
    private Custody custody;

    @JsonProperty("collateralizationTime")
    private String CollateralizationTime;

    public CollateralAsset getAsset() {
        return asset;
    }

    public Collateral setAsset(CollateralAsset asset) {
        this.asset = asset;
        return this;
    }

    public Custody getCustody() {
        return custody;
    }

    public Collateral setCustody(Custody custody) {
        this.custody = custody;
        return this;
    }

    public String getCollateralizationTime() {
        return CollateralizationTime;
    }

    public Collateral setCollateralizationTime(String collateralizationTime) {
        CollateralizationTime = collateralizationTime;
        return this;
    }

}
