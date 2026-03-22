package io.ownera.ledger.adapter.service.mapping;

import java.util.List;
import java.util.Map;

public interface AccountMappingStore {

    /**
     * Get all mappings for a finId (aggregated as key-value map).
     */
    AccountMapping getByFinId(String finId);

    /**
     * Find all finIds that have a given field value (reverse lookup).
     * E.g. find all finIds where ledgerAccountId = "0x123".
     */
    List<AccountMapping> getByFieldValue(String fieldName, String value);

    /**
     * Save all field mappings for a finId. Upserts each field.
     */
    void save(String finId, Map<String, String> fields);

    /**
     * Delete all mappings for a finId, or a specific field if fieldName is provided.
     */
    void delete(String finId, String fieldName);

    /**
     * List all mappings (aggregated per finId).
     */
    List<AccountMapping> listAll();
}
