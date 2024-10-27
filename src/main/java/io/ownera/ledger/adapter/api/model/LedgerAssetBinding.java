/*
 * Ledger Adapter Specification
 * This is the API specification for the Ledger Adapter with whom the FinP2P node will interact in order to execute and query the underlying implementation.
 *
 * The version of the OpenAPI document: x.x.x
 * Contact: support@ownera.io
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package io.ownera.ledger.adapter.api.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.ownera.ledger.adapter.api.model.LedgerTokenId;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.ownera.ledger.adapter.api.JSON;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-23T11:29:49.092442+03:00[Asia/Jerusalem]", comments = "Generator version: 7.9.0")
@JsonDeserialize(using = LedgerAssetBinding.LedgerAssetBindingDeserializer.class)
@JsonSerialize(using = LedgerAssetBinding.LedgerAssetBindingSerializer.class)
public class LedgerAssetBinding extends AbstractOpenApiSchema {
    private static final Logger log = Logger.getLogger(LedgerAssetBinding.class.getName());

    public static class LedgerAssetBindingSerializer extends StdSerializer<LedgerAssetBinding> {
        public LedgerAssetBindingSerializer(Class<LedgerAssetBinding> t) {
            super(t);
        }

        public LedgerAssetBindingSerializer() {
            this(null);
        }

        @Override
        public void serialize(LedgerAssetBinding value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeObject(value.getActualInstance());
        }
    }

    public static class LedgerAssetBindingDeserializer extends StdDeserializer<LedgerAssetBinding> {
        public LedgerAssetBindingDeserializer() {
            this(LedgerAssetBinding.class);
        }

        public LedgerAssetBindingDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public LedgerAssetBinding deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode tree = jp.readValueAsTree();
            Object deserialized = null;
            boolean typeCoercion = ctxt.isEnabled(MapperFeature.ALLOW_COERCION_OF_SCALARS);
            int match = 0;
            JsonToken token = tree.traverse(jp.getCodec()).nextToken();
            // deserialize LedgerTokenId
            try {
                boolean attemptParsing = true;
                // ensure that we respect type coercion as set on the client ObjectMapper
                if (LedgerTokenId.class.equals(Integer.class) || LedgerTokenId.class.equals(Long.class) || LedgerTokenId.class.equals(Float.class) || LedgerTokenId.class.equals(Double.class) || LedgerTokenId.class.equals(Boolean.class) || LedgerTokenId.class.equals(String.class)) {
                    attemptParsing = typeCoercion;
                    if (!attemptParsing) {
                        attemptParsing |= ((LedgerTokenId.class.equals(Integer.class) || LedgerTokenId.class.equals(Long.class)) && token == JsonToken.VALUE_NUMBER_INT);
                        attemptParsing |= ((LedgerTokenId.class.equals(Float.class) || LedgerTokenId.class.equals(Double.class)) && token == JsonToken.VALUE_NUMBER_FLOAT);
                        attemptParsing |= (LedgerTokenId.class.equals(Boolean.class) && (token == JsonToken.VALUE_FALSE || token == JsonToken.VALUE_TRUE));
                        attemptParsing |= (LedgerTokenId.class.equals(String.class) && token == JsonToken.VALUE_STRING);
                    }
                }
                if (attemptParsing) {
                    deserialized = tree.traverse(jp.getCodec()).readValueAs(LedgerTokenId.class);
                    // TODO: there is no validation against JSON schema constraints
                    // (min, max, enum, pattern...), this does not perform a strict JSON
                    // validation, which means the 'match' count may be higher than it should be.
                    match++;
                    log.log(Level.FINER, "Input data matches schema 'LedgerTokenId'");
                }
            } catch (Exception e) {
                // deserialization failed, continue
                log.log(Level.FINER, "Input data does not match schema 'LedgerTokenId'", e);
            }

            if (match == 1) {
                LedgerAssetBinding ret = new LedgerAssetBinding();
                ret.setActualInstance(deserialized);
                return ret;
            }
            throw new IOException(String.format("Failed deserialization for LedgerAssetBinding: %d classes match result, expected 1", match));
        }

        /**
         * Handle deserialization of the 'null' value.
         */
        @Override
        public LedgerAssetBinding getNullValue(DeserializationContext ctxt) throws JsonMappingException {
            throw new JsonMappingException(ctxt.getParser(), "LedgerAssetBinding cannot be null");
        }
    }

    // store a list of schema names defined in oneOf
    public static final Map<String, Class<?>> schemas = new HashMap<>();

    public LedgerAssetBinding() {
        super("oneOf", Boolean.FALSE);
    }

    public LedgerAssetBinding(LedgerTokenId o) {
        super("oneOf", Boolean.FALSE);
        setActualInstance(o);
    }

    static {
        schemas.put("LedgerTokenId", LedgerTokenId.class);
        JSON.registerDescendants(LedgerAssetBinding.class, Collections.unmodifiableMap(schemas));
        // Initialize and register the discriminator mappings.
        Map<String, Class<?>> mappings = new HashMap<String, Class<?>>();
        mappings.put("tokenId", LedgerTokenId.class);
        mappings.put("ledgerTokenId", LedgerTokenId.class);
        mappings.put("ledgerAssetBinding", LedgerAssetBinding.class);
        JSON.registerDiscriminator(LedgerAssetBinding.class, "type", mappings);
    }

    @Override
    public Map<String, Class<?>> getSchemas() {
        return LedgerAssetBinding.schemas;
    }

    /**
     * Set the instance that matches the oneOf child schema, check
     * the instance parameter is valid against the oneOf child schemas:
     * LedgerTokenId
     *
     * It could be an instance of the 'oneOf' schemas.
     * The oneOf child schemas may themselves be a composed schema (allOf, anyOf, oneOf).
     */
    @Override
    public void setActualInstance(Object instance) {
        if (JSON.isInstanceOf(LedgerTokenId.class, instance, new HashSet<Class<?>>())) {
            super.setActualInstance(instance);
            return;
        }

        throw new RuntimeException("Invalid instance type. Must be LedgerTokenId");
    }

    /**
     * Get the actual instance, which can be the following:
     * LedgerTokenId
     *
     * @return The actual instance (LedgerTokenId)
     */
    @Override
    public Object getActualInstance() {
        return super.getActualInstance();
    }

    /**
     * Get the actual instance of `LedgerTokenId`. If the actual instance is not `LedgerTokenId`,
     * the ClassCastException will be thrown.
     *
     * @return The actual instance of `LedgerTokenId`
     * @throws ClassCastException if the instance is not `LedgerTokenId`
     */
    public LedgerTokenId getLedgerTokenId() throws ClassCastException {
        return (LedgerTokenId)super.getActualInstance();
    }



  /**
   * Convert the instance into URL query string.
   *
   * @return URL query string
   */
  public String toUrlQueryString() {
    return toUrlQueryString(null);
  }

  /**
   * Convert the instance into URL query string.
   *
   * @param prefix prefix of the query string
   * @return URL query string
   */
  public String toUrlQueryString(String prefix) {
    String suffix = "";
    String containerSuffix = "";
    String containerPrefix = "";
    if (prefix == null) {
      // style=form, explode=true, e.g. /pet?name=cat&type=manx
      prefix = "";
    } else {
      // deepObject style e.g. /pet?id[name]=cat&id[type]=manx
      prefix = prefix + "[";
      suffix = "]";
      containerSuffix = "]";
      containerPrefix = "[";
    }

    StringJoiner joiner = new StringJoiner("&");

    if (getActualInstance() instanceof LedgerTokenId) {
        if (getActualInstance() != null) {
          joiner.add(((LedgerTokenId)getActualInstance()).toUrlQueryString(prefix + "one_of_0" + suffix));
        }
        return joiner.toString();
    }
    return null;
  }

}

