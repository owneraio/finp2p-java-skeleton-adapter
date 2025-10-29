package io.ownera.ledger.adapter.sample.collateral;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CollateralAsset {

    @JsonProperty("type")
    public String type;

    @JsonProperty("basket")
    public String basket;

    @JsonProperty("value")
    public String value;

    @JsonProperty("currency")
    public String currency;

    public String getType() {
        return type;
    }

    public CollateralAsset setType(String type) {
        this.type = type;
        return this;
    }

    public String getBasket() {
        return basket;
    }

    public CollateralAsset setBasket(String basket) {
        this.basket = basket;
        return this;
    }

    public String getValue() {
        return value;
    }

    public CollateralAsset setValue(String value) {
        this.value = value;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public CollateralAsset setCurrency(String currency) {
        this.currency = currency;
        return this;
    }
}
