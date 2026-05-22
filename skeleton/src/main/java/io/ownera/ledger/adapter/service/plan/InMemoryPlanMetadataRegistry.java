package io.ownera.ledger.adapter.service.plan;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link PlanMetadataRegistry}. Single-instance scope: metadata is lost on restart.
 *
 * <p>This is the default registry the skeleton wires when an adapter doesn't supply one of
 * its own. It's enough for in-process flows where plan approval and the follow-up
 * instruction executions run on the same JVM. Adapters that scale across multiple JVMs (or
 * that need to survive restarts) provide their own {@link PlanMetadataRegistry} bean —
 * typically backed by the same Postgres schema as the operations table.
 *
 * <p>Stored maps are wrapped in {@link Collections#unmodifiableMap} on read so callers can't
 * mutate the registry through the returned reference.
 */
public class InMemoryPlanMetadataRegistry implements PlanMetadataRegistry {

    private final ConcurrentHashMap<String, Map<String, Object>> store = new ConcurrentHashMap<>();

    @Override
    public void put(String planId, Map<String, Object> metadata) {
        if (planId == null) throw new IllegalArgumentException("planId must not be null");
        if (metadata == null) throw new IllegalArgumentException("metadata must not be null");
        store.put(planId, Collections.unmodifiableMap(metadata));
    }

    @Override
    public Optional<Map<String, Object>> get(@Nullable String planId) {
        if (planId == null) return Optional.empty();
        return Optional.ofNullable(store.get(planId));
    }

    @Override
    public void remove(String planId) {
        if (planId != null) store.remove(planId);
    }
}
