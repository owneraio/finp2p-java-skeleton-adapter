package io.ownera.ledger.adapter.graphql.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssetHashFunction {
    SHA256("sha3-256"),
    KECCAK_256("keccak-256");
    private final String value;

    AssetHashFunction(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static AssetHashFunction fromValue(String value) {
        for (AssetHashFunction b : AssetHashFunction.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
