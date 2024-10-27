package io.ownera.ledger.adapter.graphql.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Certificates {
    @JsonProperty("nodes")
    private List<CertificateNode> nodes;

    public List<CertificateNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<CertificateNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return "Certificates{" +
                "nodes=" + nodes +
                '}';
    }
}