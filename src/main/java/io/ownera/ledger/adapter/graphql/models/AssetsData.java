package io.ownera.ledger.adapter.graphql.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AssetsData {
    @JsonProperty("assets")
    private Assets assets;

    public Assets getAssets() {
        return assets;
    }

    public void setAssets(Assets assets) {
        this.assets = assets;
    }

    @Override
    public String toString() {
        return "Data{" +
                "assets=" + assets +
                '}';
    }
}