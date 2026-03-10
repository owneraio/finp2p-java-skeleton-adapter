package io.ownera.ledger.adapter.sample.inmemory;

import io.ownera.ledger.adapter.service.mapping.AccountMapping;
import io.ownera.ledger.adapter.service.mapping.AccountMappingStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryAccountMappingStore implements AccountMappingStore {

    private final Map<String, AccountMapping> mappings = new ConcurrentHashMap<>();

    private static String key(String finId, String account) {
        return finId + "|" + account.toLowerCase();
    }

    @Override
    public List<AccountMapping> getByFinId(String finId) {
        return mappings.values().stream()
                .filter(m -> m.getFinId().equals(finId))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountMapping> getByAccount(String account) {
        String normalized = account.toLowerCase();
        return mappings.values().stream()
                .filter(m -> m.getAccount().equals(normalized))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public AccountMapping save(String finId, String account) {
        String normalized = account.toLowerCase();
        String k = key(finId, normalized);
        return mappings.computeIfAbsent(k, _key -> {
            Instant now = Instant.now();
            return new AccountMapping(finId, normalized, now, now);
        });
    }

    @Override
    public void delete(String finId, String account) {
        if (account != null) {
            mappings.remove(key(finId, account.toLowerCase()));
        } else {
            mappings.entrySet().removeIf(e -> e.getValue().getFinId().equals(finId));
        }
    }

    @Override
    public List<AccountMapping> listAll() {
        return new ArrayList<>(mappings.values());
    }
}
