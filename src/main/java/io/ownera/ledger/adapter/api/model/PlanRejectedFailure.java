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
import io.ownera.ledger.adapter.api.model.RegulationError;
import io.ownera.ledger.adapter.api.model.RegulationFailure;
import io.ownera.ledger.adapter.api.model.ValidationFailure;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
@JsonDeserialize(using = PlanRejectedFailure.PlanRejectedFailureDeserializer.class)
@JsonSerialize(using = PlanRejectedFailure.PlanRejectedFailureSerializer.class)
public class PlanRejectedFailure extends AbstractOpenApiSchema {
    private static final Logger log = Logger.getLogger(PlanRejectedFailure.class.getName());

    public static class PlanRejectedFailureSerializer extends StdSerializer<PlanRejectedFailure> {
        public PlanRejectedFailureSerializer(Class<PlanRejectedFailure> t) {
            super(t);
        }

        public PlanRejectedFailureSerializer() {
            this(null);
        }

        @Override
        public void serialize(PlanRejectedFailure value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeObject(value.getActualInstance());
        }
    }

    public static class PlanRejectedFailureDeserializer extends StdDeserializer<PlanRejectedFailure> {
        public PlanRejectedFailureDeserializer() {
            this(PlanRejectedFailure.class);
        }

        public PlanRejectedFailureDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public PlanRejectedFailure deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode tree = jp.readValueAsTree();
            Object deserialized = null;
            boolean typeCoercion = ctxt.isEnabled(MapperFeature.ALLOW_COERCION_OF_SCALARS);
            int match = 0;
            JsonToken token = tree.traverse(jp.getCodec()).nextToken();
            // deserialize RegulationFailure
            try {
                boolean attemptParsing = true;
                // ensure that we respect type coercion as set on the client ObjectMapper
                if (RegulationFailure.class.equals(Integer.class) || RegulationFailure.class.equals(Long.class) || RegulationFailure.class.equals(Float.class) || RegulationFailure.class.equals(Double.class) || RegulationFailure.class.equals(Boolean.class) || RegulationFailure.class.equals(String.class)) {
                    attemptParsing = typeCoercion;
                    if (!attemptParsing) {
                        attemptParsing |= ((RegulationFailure.class.equals(Integer.class) || RegulationFailure.class.equals(Long.class)) && token == JsonToken.VALUE_NUMBER_INT);
                        attemptParsing |= ((RegulationFailure.class.equals(Float.class) || RegulationFailure.class.equals(Double.class)) && token == JsonToken.VALUE_NUMBER_FLOAT);
                        attemptParsing |= (RegulationFailure.class.equals(Boolean.class) && (token == JsonToken.VALUE_FALSE || token == JsonToken.VALUE_TRUE));
                        attemptParsing |= (RegulationFailure.class.equals(String.class) && token == JsonToken.VALUE_STRING);
                    }
                }
                if (attemptParsing) {
                    deserialized = tree.traverse(jp.getCodec()).readValueAs(RegulationFailure.class);
                    // TODO: there is no validation against JSON schema constraints
                    // (min, max, enum, pattern...), this does not perform a strict JSON
                    // validation, which means the 'match' count may be higher than it should be.
                    match++;
                    log.log(Level.FINER, "Input data matches schema 'RegulationFailure'");
                }
            } catch (Exception e) {
                // deserialization failed, continue
                log.log(Level.FINER, "Input data does not match schema 'RegulationFailure'", e);
            }

            // deserialize ValidationFailure
            try {
                boolean attemptParsing = true;
                // ensure that we respect type coercion as set on the client ObjectMapper
                if (ValidationFailure.class.equals(Integer.class) || ValidationFailure.class.equals(Long.class) || ValidationFailure.class.equals(Float.class) || ValidationFailure.class.equals(Double.class) || ValidationFailure.class.equals(Boolean.class) || ValidationFailure.class.equals(String.class)) {
                    attemptParsing = typeCoercion;
                    if (!attemptParsing) {
                        attemptParsing |= ((ValidationFailure.class.equals(Integer.class) || ValidationFailure.class.equals(Long.class)) && token == JsonToken.VALUE_NUMBER_INT);
                        attemptParsing |= ((ValidationFailure.class.equals(Float.class) || ValidationFailure.class.equals(Double.class)) && token == JsonToken.VALUE_NUMBER_FLOAT);
                        attemptParsing |= (ValidationFailure.class.equals(Boolean.class) && (token == JsonToken.VALUE_FALSE || token == JsonToken.VALUE_TRUE));
                        attemptParsing |= (ValidationFailure.class.equals(String.class) && token == JsonToken.VALUE_STRING);
                    }
                }
                if (attemptParsing) {
                    deserialized = tree.traverse(jp.getCodec()).readValueAs(ValidationFailure.class);
                    // TODO: there is no validation against JSON schema constraints
                    // (min, max, enum, pattern...), this does not perform a strict JSON
                    // validation, which means the 'match' count may be higher than it should be.
                    match++;
                    log.log(Level.FINER, "Input data matches schema 'ValidationFailure'");
                }
            } catch (Exception e) {
                // deserialization failed, continue
                log.log(Level.FINER, "Input data does not match schema 'ValidationFailure'", e);
            }

            if (match == 1) {
                PlanRejectedFailure ret = new PlanRejectedFailure();
                ret.setActualInstance(deserialized);
                return ret;
            }
            throw new IOException(String.format("Failed deserialization for PlanRejectedFailure: %d classes match result, expected 1", match));
        }

        /**
         * Handle deserialization of the 'null' value.
         */
        @Override
        public PlanRejectedFailure getNullValue(DeserializationContext ctxt) throws JsonMappingException {
            throw new JsonMappingException(ctxt.getParser(), "PlanRejectedFailure cannot be null");
        }
    }

    // store a list of schema names defined in oneOf
    public static final Map<String, Class<?>> schemas = new HashMap<>();

    public PlanRejectedFailure() {
        super("oneOf", Boolean.FALSE);
    }

    public PlanRejectedFailure(RegulationFailure o) {
        super("oneOf", Boolean.FALSE);
        setActualInstance(o);
    }

    public PlanRejectedFailure(ValidationFailure o) {
        super("oneOf", Boolean.FALSE);
        setActualInstance(o);
    }

    static {
        schemas.put("RegulationFailure", RegulationFailure.class);
        schemas.put("ValidationFailure", ValidationFailure.class);
        JSON.registerDescendants(PlanRejectedFailure.class, Collections.unmodifiableMap(schemas));
        // Initialize and register the discriminator mappings.
        Map<String, Class<?>> mappings = new HashMap<String, Class<?>>();
        mappings.put("regulationFailure", RegulationFailure.class);
        mappings.put("validationFailure", ValidationFailure.class);
        mappings.put("RegulationFailure", RegulationFailure.class);
        mappings.put("ValidationFailure", ValidationFailure.class);
        mappings.put("PlanRejected_failure", PlanRejectedFailure.class);
        JSON.registerDiscriminator(PlanRejectedFailure.class, "failureType", mappings);
    }

    @Override
    public Map<String, Class<?>> getSchemas() {
        return PlanRejectedFailure.schemas;
    }

    /**
     * Set the instance that matches the oneOf child schema, check
     * the instance parameter is valid against the oneOf child schemas:
     * RegulationFailure, ValidationFailure
     *
     * It could be an instance of the 'oneOf' schemas.
     * The oneOf child schemas may themselves be a composed schema (allOf, anyOf, oneOf).
     */
    @Override
    public void setActualInstance(Object instance) {
        if (JSON.isInstanceOf(RegulationFailure.class, instance, new HashSet<Class<?>>())) {
            super.setActualInstance(instance);
            return;
        }

        if (JSON.isInstanceOf(ValidationFailure.class, instance, new HashSet<Class<?>>())) {
            super.setActualInstance(instance);
            return;
        }

        throw new RuntimeException("Invalid instance type. Must be RegulationFailure, ValidationFailure");
    }

    /**
     * Get the actual instance, which can be the following:
     * RegulationFailure, ValidationFailure
     *
     * @return The actual instance (RegulationFailure, ValidationFailure)
     */
    @Override
    public Object getActualInstance() {
        return super.getActualInstance();
    }

    /**
     * Get the actual instance of `RegulationFailure`. If the actual instance is not `RegulationFailure`,
     * the ClassCastException will be thrown.
     *
     * @return The actual instance of `RegulationFailure`
     * @throws ClassCastException if the instance is not `RegulationFailure`
     */
    public RegulationFailure getRegulationFailure() throws ClassCastException {
        return (RegulationFailure)super.getActualInstance();
    }

    /**
     * Get the actual instance of `ValidationFailure`. If the actual instance is not `ValidationFailure`,
     * the ClassCastException will be thrown.
     *
     * @return The actual instance of `ValidationFailure`
     * @throws ClassCastException if the instance is not `ValidationFailure`
     */
    public ValidationFailure getValidationFailure() throws ClassCastException {
        return (ValidationFailure)super.getActualInstance();
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

    if (getActualInstance() instanceof ValidationFailure) {
        if (getActualInstance() != null) {
          joiner.add(((ValidationFailure)getActualInstance()).toUrlQueryString(prefix + "one_of_0" + suffix));
        }
        return joiner.toString();
    }
    if (getActualInstance() instanceof RegulationFailure) {
        if (getActualInstance() != null) {
          joiner.add(((RegulationFailure)getActualInstance()).toUrlQueryString(prefix + "one_of_1" + suffix));
        }
        return joiner.toString();
    }
    return null;
  }

}
