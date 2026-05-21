package io.ownera.ledger.adapter.service.workflow;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * Serializes method-argument tuples to JSON (for {@code operations.inputs} persistence) and
 * deserializes them back on crash recovery.
 *
 * <p>Default typing is enabled so polymorphic argument types (e.g. {@code Source}/{@code Destination}
 * via {@code SourceAccount}/{@code DestinationAccount} sub-interfaces, {@code AssetBind} variants)
 * round-trip with their concrete class names. The validator is {@code LaissezFaireSubTypeValidator}
 * because the JSON is only ever read back from our own database — there is no untrusted-input
 * threat model.
 *
 * <p>Field-based access mirrors the rest of the skeleton's models, which expose {@code public final}
 * fields rather than getters.
 */
public class WorkflowArgsCodec {

    private final ObjectMapper mapper;

    public WorkflowArgsCodec() {
        this(buildMapper());
    }

    public WorkflowArgsCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper m = new ObjectMapper();
        // jackson-module-parameter-names lets us deserialize immutable POJOs (public final fields
        // + single all-args constructor, no @JsonCreator) provided the skeleton is compiled with
        // `-parameters` so constructor parameter names survive into bytecode. Register explicitly
        // (auto-discovery via findAndRegisterModules() turned out not to wire it in this setup).
        m.registerModule(new ParameterNamesModule());
        // When a class has multiple constructors (e.g. {@code Asset} has a 2-arg overload that
        // delegates to the canonical 3-arg one), Jackson by default refuses to pick. Force it to
        // prefer the canonical (largest-arity) constructor for property-based binding.
        m.setConstructorDetector(ConstructorDetector.USE_PROPERTIES_BASED);
        m.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        m.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
        m.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return m;
    }

    public String encode(Object[] args) {
        try {
            return mapper.writeValueAsString(args);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize workflow args", e);
        }
    }

    public Object[] decode(String json) {
        try {
            return mapper.readValue(json, Object[].class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize workflow args", e);
        }
    }
}
