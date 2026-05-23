package io.ownera.ledger.adapter.plan;

import io.ownera.finp2p.OperationalSDK;
import io.ownera.finp2p.opapi.model.Execution;
import io.ownera.finp2p.opapi.model.ExecutionPlan;
import io.ownera.ledger.adapter.service.model.ApprovedPlan;
import io.ownera.ledger.adapter.service.model.PlanApprovalStatus;
import io.ownera.ledger.adapter.service.plan.DefaultPlanApprovalService;
import io.ownera.ledger.adapter.service.plan.InMemoryPlanMetadataRegistry;
import io.ownera.ledger.adapter.service.plan.PlanAnalyzer;
import io.ownera.ledger.adapter.service.plan.PlanMetadataRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link PlanAnalyzer} hook + {@link PlanMetadataRegistry} integration with
 * {@link DefaultPlanApprovalService}.
 */
class PlanAnalyzerTest {

    private OperationalSDK sdk;
    private PlanMetadataRegistry registry;

    @BeforeEach
    void setup() {
        sdk = Mockito.mock(OperationalSDK.class);
        registry = new InMemoryPlanMetadataRegistry();
    }

    private DefaultPlanApprovalService service(PlanAnalyzer analyzer) {
        return new DefaultPlanApprovalService(
                "org-test", sdk, null, null, analyzer, registry);
    }

    /**
     * Build a minimal Execution+ExecutionPlan with the given id. Instructions list is empty so
     * the validatePlan loop is a no-op and the test focuses on the analyzer hook.
     */
    private static Execution executionWith(String planId) throws Exception {
        ExecutionPlan plan = new ExecutionPlan();
        plan.setId(planId);
        plan.setInstructions(java.util.Collections.emptyList());
        Execution exec = new Execution();
        exec.setPlan(plan);
        return exec;
    }

    @Test
    void analyzerIsInvokedAndResultIsStoredInRegistry() throws Exception {
        String planId = "plan-" + System.nanoTime();
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(executionWith(planId));

        Map<String, Object> meta = new HashMap<>();
        meta.put("isInbound", true);
        meta.put("counterpartyOrg", "org-counterparty");
        PlanAnalyzer analyzer = plan -> meta;

        DefaultPlanApprovalService svc = service(analyzer);
        PlanApprovalStatus status = svc.approvePlan("ik-" + System.nanoTime(), planId);

        assertTrue(status instanceof ApprovedPlan,
                "no plugin + no instructions → ApprovedPlan, got " + status.getClass());

        Optional<Map<String, Object>> stored = registry.get(planId);
        assertTrue(stored.isPresent(), "analyzer metadata must be stashed under planId");
        assertEquals(true, stored.get().get("isInbound"));
        assertEquals("org-counterparty", stored.get().get("counterpartyOrg"));
    }

    @Test
    void registryIsLeftEmptyWhenNoAnalyzerIsWired() throws Exception {
        String planId = "plan-noop-" + System.nanoTime();
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(executionWith(planId));

        DefaultPlanApprovalService svc = service(null);
        svc.approvePlan("ik-" + System.nanoTime(), planId);

        assertFalse(registry.get(planId).isPresent(),
                "no analyzer wired → no entry in the registry");
    }

    @Test
    void approvalContinuesWhenAnalyzerThrows() throws Exception {
        // A buggy analyzer must not tank the plan-approval response. Exception is contained,
        // logged, and approval proceeds; the registry stays empty for this plan.
        String planId = "plan-throw-" + System.nanoTime();
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(executionWith(planId));

        PlanAnalyzer faulty = plan -> { throw new RuntimeException("analyzer is broken"); };
        DefaultPlanApprovalService svc = service(faulty);

        PlanApprovalStatus status = svc.approvePlan("ik-" + System.nanoTime(), planId);
        assertTrue(status instanceof ApprovedPlan, "approval must survive analyzer exception");
        assertFalse(registry.get(planId).isPresent(),
                "thrown analyzer → no registry entry (intentional: garbage in, no garbage out)");
    }

    @Test
    void registryHoldsMetadataForMultiplePlansIndependently() throws Exception {
        String planA = "plan-A-" + System.nanoTime();
        String planB = "plan-B-" + System.nanoTime();
        Mockito.when(sdk.getExecutionPlan(planA)).thenReturn(executionWith(planA));
        Mockito.when(sdk.getExecutionPlan(planB)).thenReturn(executionWith(planB));

        // Analyzer returns different metadata per plan so we can check cross-plan isolation.
        PlanAnalyzer analyzer = plan -> {
            Map<String, Object> m = new HashMap<>();
            m.put("planId", plan.getId());
            return m;
        };
        DefaultPlanApprovalService svc = service(analyzer);
        svc.approvePlan("ik-A", planA);
        svc.approvePlan("ik-B", planB);

        assertEquals(planA, registry.get(planA).orElseThrow().get("planId"));
        assertEquals(planB, registry.get(planB).orElseThrow().get("planId"));
    }

    @Test
    void registryRemoveEvictsEntry() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("foo", "bar");
        registry.put("plan-evict", meta);
        assertTrue(registry.get("plan-evict").isPresent());

        registry.remove("plan-evict");
        assertFalse(registry.get("plan-evict").isPresent());

        // Removing a non-existent key is a no-op (no throw).
        assertDoesNotThrow(() -> registry.remove("plan-evict"));
        assertDoesNotThrow(() -> registry.remove("plan-does-not-exist"));
    }

    @Test
    void registryGetTolaratesNullPlanId() {
        assertFalse(registry.get(null).isPresent());
    }

    @Test
    void inMemoryRegistryWrapsStoredMetadataReadOnly() {
        // Stored maps must be immutable from the caller's perspective so a caller can't mutate
        // the registry's internal state via the returned reference.
        Map<String, Object> meta = new HashMap<>();
        meta.put("k", "v");
        registry.put("plan-immutable", meta);

        Map<String, Object> got = registry.get("plan-immutable").orElseThrow();
        assertThrows(UnsupportedOperationException.class, () -> got.put("k2", "v2"));
    }

    @Test
    void inMemoryRegistrySnapshotsMetadataOnWrite() {
        // Reviewer-flagged: the previous impl wrapped the caller's map with unmodifiableMap
        // without copying. Mutating the source after put() then leaked into the registry.
        // The fix snapshots on write, so post-put mutations are invisible to readers.
        Map<String, Object> caller = new HashMap<>();
        caller.put("version", 1);
        registry.put("plan-snapshot", caller);

        // Caller continues to own + mutate their map after put().
        caller.put("version", 2);
        caller.put("added-after-put", "should-not-leak");

        Map<String, Object> stored = registry.get("plan-snapshot").orElseThrow();
        assertEquals(1, stored.get("version"),
                "post-put mutation must not retroactively change the stored value");
        assertNull(stored.get("added-after-put"),
                "post-put keys must not appear in the registry");
    }

    @Test
    void inMemoryRegistryIsolatesPlansWhenAnalyzerReusesSameMap() {
        // A perfectly legitimate analyzer might hold a single Map instance as a field and
        // mutate it per call. Without the defensive copy, the second put() would replace the
        // first plan's entry with the second's contents and ongoing mutations would corrupt
        // both. The fix guarantees each put() captures the map's state at that moment.
        Map<String, Object> reused = new HashMap<>();

        reused.clear(); reused.put("planId", "A"); reused.put("counter", 1);
        registry.put("plan-A", reused);

        reused.clear(); reused.put("planId", "B"); reused.put("counter", 2);
        registry.put("plan-B", reused);

        Map<String, Object> a = registry.get("plan-A").orElseThrow();
        Map<String, Object> b = registry.get("plan-B").orElseThrow();
        assertEquals("A", a.get("planId"), "plan-A's snapshot must not be overwritten by plan-B's put()");
        assertEquals(1, a.get("counter"));
        assertEquals("B", b.get("planId"));
        assertEquals(2, b.get("counter"));
    }

    @Test
    void backCompatConstructorDefaultsToInMemoryRegistryAndNoAnalyzer() throws Exception {
        // 4-arg constructor: no analyzer is called, and the registry the service uses
        // internally is the in-memory default.
        String planId = "plan-back-compat-" + System.nanoTime();
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(executionWith(planId));

        DefaultPlanApprovalService svc = new DefaultPlanApprovalService(
                "org-test", sdk, null, null);
        PlanApprovalStatus status = svc.approvePlan("ik-" + System.nanoTime(), planId);
        assertTrue(status instanceof ApprovedPlan);

        // The service exposes its internal registry; with no analyzer, nothing was stashed.
        assertFalse(svc.getPlanMetadataRegistry().get(planId).isPresent());
    }
}
