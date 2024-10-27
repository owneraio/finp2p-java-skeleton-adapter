package io.ownera.ledger.adapter.graphql.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Assets {
    @JsonProperty("nodes")
    private List<AssetDetails> nodes;

    public List<AssetDetails> getNodes() {
        return nodes;
    }

    public void setNodes(List<AssetDetails> nodes) {
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return "Assets{" +
                "nodes=" + nodes +
                '}';
    }
}