package io.ownera.ledger.adapter.graphql.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class AssetDetails {
    @JsonProperty("id")
    private String id;

    @JsonProperty("config")
    private String config;

    @JsonProperty("regulationVerifiers")
    private List<RegulationVerifier> regulationVerifiers;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<RegulationVerifier> getRegulationVerifiers() {
        return regulationVerifiers;
    }

    public void setRegulationVerifiers(List<RegulationVerifier> regulationVerifiers) {
        this.regulationVerifiers = regulationVerifiers;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public AssetConfig parseAssetConfig() throws IOException {
        return new ObjectMapper().readValue(config, AssetConfig.class);
    }

    @Override
    public String toString() {
        return "AssetNode{" +
                "id='" + id + '\'' +
                ", config=" + config +
                ", regulationVerifiers=" + regulationVerifiers +
                '}';
    }
}