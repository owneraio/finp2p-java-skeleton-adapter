package io.ownera.ledger.adapter.service.mapping;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default {@link AccountMappingService} implementation — thin delegate over
 * {@link AccountMappingStore} with optional {@link MappingValidator}.
 */
public class DefaultAccountMappingService implements AccountMappingService {

    private final AccountMappingStore store;
    private final Optional<MappingValidator> validator;

    public DefaultAccountMappingService(AccountMappingStore store) {
        this(store, Optional.empty());
    }

    public DefaultAccountMappingService(AccountMappingStore store, Optional<MappingValidator> validator) {
        this.store = store;
        this.validator = validator;
    }

    @Override
    public List<AccountMapping> getAccounts(@Nullable List<String> finIds) {
        if (finIds == null || finIds.isEmpty()) {
            return store.listAll();
        }
        return finIds.stream()
                .map(store::getByFinId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountMapping> getByFieldValue(String fieldName, String value) {
        return store.getByFieldValue(fieldName, value);
    }

    @Override
    public AccountMapping saveAccount(String finId, Map<String, String> fields) {
        Map<String, String> validated = validator
                .map(v -> v.validate(finId, fields))
                .orElse(fields);
        store.save(finId, validated);
        return store.getByFinId(finId);
    }

    @Override
    public void deleteAccount(String finId, @Nullable String fieldName) {
        store.delete(finId, fieldName);
    }
}
