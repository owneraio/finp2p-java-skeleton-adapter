package io.ownera.ledger.adapter.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ownera.ledger.adapter.Mappers;
import io.ownera.ledger.adapter.api.model.APIOperationStatus;
import io.ownera.ledger.adapter.service.model.OperationStatus;

/**
 * Serializes an internal {@link OperationStatus} into the wire-format JSON
 * (an {@link APIOperationStatus}) for durable storage in
 * {@code operations.outputs}.
 *
 * <p>The default implementation goes through {@link Mappers#toAPI(OperationStatus)}
 * so persisted outputs are byte-identical to what the controller returns from
 * {@code GET /api/operations/status/{cid}} — the polling endpoint can serve the
 * stored JSON directly without round-tripping.
 *
 * <p>Tests / custom executors may inject a different serializer to control
 * format or fail-fast behavior.
 */
@FunctionalInterface
public interface OperationOutputSerializer {

    /**
     * @param result completed operation result (never {@code null})
     * @return JSON string, or {@code null} when the result cannot be serialized
     *         (e.g. an in-test stub)
     */
    String serialize(OperationStatus result);

    /**
     * Default serializer: {@code Mappers.toAPI(result)} + Jackson.
     */
    static OperationOutputSerializer defaultSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        return result -> {
            try {
                APIOperationStatus api = Mappers.toAPI(result);
                return mapper.writeValueAsString(api);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize OperationStatus", e);
            }
        };
    }
}
