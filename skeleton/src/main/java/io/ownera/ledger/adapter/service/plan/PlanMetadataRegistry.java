package io.ownera.ledger.adapter.service.plan;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * Tiny key-value store keyed by {@code planId}, holding the metadata an adapter's
 * {@link PlanAnalyzer} produces during plan approval. Operation handlers (e.g. inside the
 * Controller) look the metadata up later when the plan's instructions actually execute.
 *
 * <p>Default impl is {@link InMemoryPlanMetadataRegistry} — a ConcurrentHashMap. Adapters
 * that need cross-process durability swap in their own implementation (e.g. backed by the
 * skeleton's operations schema).
 *
 * <p>The registry is intentionally narrow: no TTL, no automatic eviction. Adapters call
 * {@link #remove(String)} themselves when a plan is final — either in
 * {@code PlanApprovalService.proposalStatus} or when the last instruction finalizes.
 */
public interface PlanMetadataRegistry {

    /** Stash {@code metadata} for {@code planId}. Replaces any prior value. */
    void put(String planId, Map<String, Object> metadata);

    /** @return the metadata if present, empty otherwise. */
    Optional<Map<String, Object>> get(@Nullable String planId);

    /** Remove the metadata for {@code planId}. No-op if absent. */
    void remove(String planId);
}
