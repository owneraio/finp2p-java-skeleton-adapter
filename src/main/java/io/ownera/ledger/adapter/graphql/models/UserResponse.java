package io.ownera.ledger.adapter.graphql.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserResponse {
    @JsonProperty("data")
    private UsersData data;

    public UsersData getData() {
        return data;
    }

    public void setData(UsersData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "UserResponse{" +
                "data=" + data +
                '}';
    }
}