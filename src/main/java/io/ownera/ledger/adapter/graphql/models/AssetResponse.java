package io.ownera.ledger.adapter.graphql.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AssetResponse {
    @JsonProperty("data")
    private AssetsData data;

    public AssetsData getData() {
        return data;
    }

    public void setData(AssetsData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "AssetResponse{" +
                "data=" + data +
                '}';
    }
}