package io.ownera.ledger.adapter.service.mapping;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Service-layer contract for account mapping operations.
 * Aligns with Node.js skeleton's {@code AccountMappingService}.
 * <p>
 * Thin service layer over {@link AccountMappingStore}: adapters can implement this
 * directly to inject business logic (validation, lookup augmentation, etc.)
 * or use {@link DefaultAccountMappingService} which delegates to a store.
 */
public interface AccountMappingService {

    /**
     * Get mappings for the given finIds, or all if {@code finIds} is null/empty.
     */
    List<AccountMapping> getAccounts(@Nullable List<String> finIds);

    /**
     * Reverse lookup: find finIds with a given field value.
     */
    List<AccountMapping> getByFieldValue(String fieldName, String value);

    /**
     * Upsert all field mappings for a finId.
     * @return the persisted mapping (may differ from input if validator transformed fields)
     */
    AccountMapping saveAccount(String finId, Map<String, String> fields);

    /**
     * Delete all mappings for a finId, or a specific field if {@code fieldName} is provided.
     */
    void deleteAccount(String finId, @Nullable String fieldName);
}
