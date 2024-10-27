package io.ownera.ledger.adapter.graphql.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Users {
    @JsonProperty("nodes")
    private List<UserDetails> nodes;

    public List<UserDetails> getNodes() {
        return nodes;
    }

    public void setNodes(List<UserDetails> nodes) {
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return "Users{" +
                "nodes=" + nodes +
                '}';
    }
}