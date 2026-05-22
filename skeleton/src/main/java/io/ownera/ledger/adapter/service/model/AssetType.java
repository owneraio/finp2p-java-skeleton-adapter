package io.ownera.ledger.adapter.service.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssetType {
    FINP2P,
    FIAT,
    CRYPTOCURRENCY;

    /**
     * Wire form is the lowercase enum name — matches the Node skeleton's
     * {@code AssetType = 'finp2p' | 'fiat' | 'cryptocurrency'} string-literal union so requests
     * and responses round-trip cleanly across both implementations.
     */
    @JsonValue
    public String toWire() {
        return name().toLowerCase();
    }

    /**
     * Lenient parse: accepts both {@code "finp2p"} (Node-style) and {@code "FINP2P"} (the
     * legacy JSON shape, before this annotation was added). Mixed casing also works. Existing
     * persisted JSONB rows (e.g. workflow proxy inputs from earlier builds) keep deserializing.
     */
    @JsonCreator
    public static AssetType fromWire(String value) {
        if (value == null) {
            throw new IllegalArgumentException("AssetType wire value cannot be null");
        }
        try {
            return AssetType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown AssetType: " + value, e);
        }
    }
}
