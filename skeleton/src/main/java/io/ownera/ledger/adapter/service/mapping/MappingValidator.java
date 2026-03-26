package io.ownera.ledger.adapter.service.mapping;

import java.util.Map;

/**
 * Optional validation for account mappings.
 * Adapters implement this to validate or transform fields before they are persisted.
 * Throw IllegalArgumentException to reject the mapping.
 */
public interface MappingValidator {

    /**
     * Validate and optionally transform account mapping fields.
     *
     * @param finId  the FinP2P identity
     * @param fields the raw fields from the request
     * @return the validated (possibly transformed) fields to persist
     * @throws IllegalArgumentException if the mapping is invalid
     */
    Map<String, String> validate(String finId, Map<String, String> fields);
}
