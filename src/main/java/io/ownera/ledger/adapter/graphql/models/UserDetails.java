package io.ownera.ledger.adapter.graphql.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDetails {
    @JsonProperty("id")
    private String id;

    @JsonProperty("certificates")
    private Certificates certificates;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public Certificates getCertificates() {
        return certificates;
    }

    public void setCertificates(Certificates certificates) {
        this.certificates = certificates;
    }

    @Override
    public String toString() {
        return "UserNode{" +
                "id='" + id + '\'' +
                ", certificates=" + certificates +
                '}';
    }
}