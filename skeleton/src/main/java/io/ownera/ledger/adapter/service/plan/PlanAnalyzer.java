package io.ownera.ledger.adapter.service.plan;

import io.ownera.finp2p.opapi.model.ExecutionPlan;

import java.util.Map;

/**
 * Adapter-provided hook called during plan approval to derive metadata about an
 * {@link ExecutionPlan}. The returned map is stashed in the
 * {@link PlanMetadataRegistry} under the plan's id, where adapter-side code that handles
 * individual operations later (e.g. {@code Controller.issue}, {@code Controller.transfer})
 * can look it up by {@code planId} and use it to enrich its execution context.
 *
 * <p>Mirrors Node's {@code PlanAnalyzer} interface
 * ({@code skeleton/src/models/plugins/interfaces.ts}): {@code analyzePlan(plan) →
 * Record<string, any>}. Node declares the hook but never invokes it; Java actually wires the
 * call site in {@link DefaultPlanApprovalService#approvePlan} so adapters get a usable
 * extension point rather than a dead stub.
 *
 * <p>Implementations should be cheap — this runs inline during plan approval, so a slow
 * analyzer slows every approval. Long-running work belongs in a separate async pipeline that
 * the analyzer kicks off.
 */
@FunctionalInterface
public interface PlanAnalyzer {

    /**
     * @param plan the execution plan being approved (already fetched from FinP2P).
     * @return free-form metadata to associate with this plan. Never {@code null} — return an
     *         empty map if there's nothing to attach.
     */
    Map<String, Object> analyzePlan(ExecutionPlan plan);
}
