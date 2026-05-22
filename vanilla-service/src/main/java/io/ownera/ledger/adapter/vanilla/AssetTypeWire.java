package io.ownera.ledger.adapter.vanilla;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.ownera.ledger.adapter.service.model.AssetType;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Wire-format converters for {@link AssetType} on the {@code /distribution/*} HTTP surface.
 *
 * <p>Node's distribution contract uses lowercase string literals ({@code "finp2p" | "fiat" |
 * "cryptocurrency"}). Java's default Jackson enum binding would emit and require the uppercase
 * {@code name()} ({@code "FINP2P"}). To bridge that — without changing the global wire shape
 * of {@link AssetType} (the workflow proxy hashes its serialized form into
 * {@code inputs_hash}; flipping that across an upgrade would silently break idempotent
 * replay) — the distribution DTOs annotate their {@code assetType} field with
 * {@link Serializer} / {@link Deserializer}.
 *
 * <p>Deserialization is lenient: accepts both casings so an adapter that has already been
 * sending the Node-style lowercase form keeps working, and clients/tests still pinning to
 * uppercase also keep working. Serialization is always lowercase.
 */
public final class AssetTypeWire {

    private AssetTypeWire() {}

    public static AssetType parse(@Nullable String value) {
        if (value == null) {
            throw new IllegalArgumentException("assetType cannot be null");
        }
        try {
            return AssetType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown assetType: " + value, e);
        }
    }

    public static String toWire(AssetType type) {
        return type.name().toLowerCase();
    }

    public static final class Serializer extends JsonSerializer<AssetType> {
        @Override
        public void serialize(AssetType value, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
            gen.writeString(toWire(value));
        }
    }

    public static final class Deserializer extends JsonDeserializer<AssetType> {
        @Override
        public AssetType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return parse(p.getValueAsString());
        }
    }
}
