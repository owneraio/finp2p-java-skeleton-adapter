package io.ownera.ledger.adapter.service.plan;

import io.ownera.finp2p.OperationalSDK;
import io.ownera.finp2p.opapi.model.Execution;
import io.ownera.finp2p.opapi.model.ExecutionInstruction;
import io.ownera.finp2p.opapi.model.ExecutionPlan;
import io.ownera.finp2p.opapi.model.ExecutionPlanOperation;
import io.ownera.finp2p.opapi.model.FinIdAccount1;
import io.ownera.finp2p.opapi.model.Finp2pAsset;
import io.ownera.finp2p.opapi.model.Finp2pAssetAccount;
import io.ownera.finp2p.opapi.model.LedgerAccountAsset;
import io.ownera.finp2p.opapi.model.ReleaseInstruction;
import io.ownera.finp2p.opapi.model.TransferInstruction;
import io.ownera.ledger.adapter.service.model.ApprovedPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The inbound-transfer hook must fire on the <em>receiving</em> side — the org that owns the
 * destination asset — not on the executing/sending org. Earlier the gate used the
 * instruction's {@code organizations} list (the sender) and so fired the inbound hook on the
 * wrong adapter. These tests pin the corrected gate: {@code orgIdFromResource(destAsset)} ==
 * our org, for both the approval path ({@code onPlannedInboundTransfer}) and the
 * instruction-proposal path ({@code onInboundTransfer}), across both {@code transfer} and
 * {@code release} operations.
 */
class InboundTransferOrgGateTest {

    private OperationalSDK sdk;

    @BeforeEach
    void setup() {
        sdk = Mockito.mock(OperationalSDK.class);
    }

    // ── Path B: proposeInstructionApproval ──────────────────────────────

    @Test
    void onInboundTransferFiresWhenWeOwnDestinationAsset() throws Exception {
        // dest asset org == our org → hook fires.
        String planId = "plan-" + System.nanoTime();
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(
                transferExecution(planId, 7, "org-other:102:src", "org-me:102:dst"));

        AtomicReference<InboundTransferHook.InboundTransferContext> seen = new AtomicReference<>();
        svc("org-me", capture(seen)).proposeInstructionApproval("ik", planId, 7);

        assertNotNull(seen.get(), "hook must fire when we own the destination asset");
        assertEquals(planId, seen.get().planId);
    }

    @Test
    void onInboundTransferDoesNotFireWhenAnotherOrgOwnsDestinationAsset() throws Exception {
        // dest asset org != our org → hook must NOT fire (we're the sender, not the receiver).
        String planId = "plan-" + System.nanoTime();
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(
                transferExecution(planId, 7, "org-me:102:src", "org-other:102:dst"));

        AtomicReference<InboundTransferHook.InboundTransferContext> seen = new AtomicReference<>();
        svc("org-me", capture(seen)).proposeInstructionApproval("ik", planId, 7);

        assertNull(seen.get(), "hook must not fire on the sending side");
    }

    @Test
    void onInboundTransferFiresForReleaseToOurOrg() throws Exception {
        // release with dest asset org == our org → hook fires (release has inbound semantics).
        String planId = "plan-" + System.nanoTime();
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(
                releaseExecution(planId, 8, "org-other:102:src", "org-me:102:dst"));

        AtomicReference<InboundTransferHook.InboundTransferContext> seen = new AtomicReference<>();
        svc("org-me", capture(seen)).proposeInstructionApproval("ik", planId, 8);

        assertNotNull(seen.get(), "release to our org must fire the inbound hook");
        assertEquals("org-me:102:dst", seen.get().asset.assetId);
    }

    // ── Path A: approvePlan → validatePlan → onPlannedInboundTransfer ───

    @Test
    void onPlannedInboundTransferFiresWhenWeOwnDestinationAsset() throws Exception {
        String planId = "plan-" + System.nanoTime();
        // instruction assigned to our org AND dest asset is ours → planned hook fires.
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(
                transferExecutionAssignedTo(planId, 1, "org-me", "org-other:102:src", "org-me:102:dst"));

        AtomicReference<InboundTransferHook.PlannedInboundTransferContext> seen = new AtomicReference<>();
        svc("org-me", capturePlanned(seen)).approvePlan("ik", planId);

        assertNotNull(seen.get(), "planned hook must fire when we own the destination asset");
        assertEquals("org-me:102:dst", seen.get().asset.assetId);
    }

    @Test
    void onPlannedInboundTransferDoesNotFireWhenAnotherOrgOwnsDestinationAsset() throws Exception {
        String planId = "plan-" + System.nanoTime();
        // instruction assigned to our org (we execute it) but dest asset is another org →
        // we're the sender, planned inbound hook must NOT fire.
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(
                transferExecutionAssignedTo(planId, 1, "org-me", "org-me:102:src", "org-other:102:dst"));

        AtomicReference<InboundTransferHook.PlannedInboundTransferContext> seen = new AtomicReference<>();
        var status = svc("org-me", capturePlanned(seen)).approvePlan("ik", planId);

        assertNull(seen.get(), "planned hook must not fire on the sending side");
        assertTrue(status instanceof ApprovedPlan);
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private DefaultPlanApprovalService svc(String orgId, InboundTransferHook hook) {
        return new DefaultPlanApprovalService(orgId, sdk, null, hook);
    }

    private static InboundTransferHook capture(AtomicReference<InboundTransferHook.InboundTransferContext> sink) {
        return new InboundTransferHook() {
            @Override public void onPlannedInboundTransfer(String ik, PlannedInboundTransferContext ctx) {}
            @Override public void onInboundTransfer(String ik, InboundTransferContext ctx) { sink.set(ctx); }
        };
    }

    private static InboundTransferHook capturePlanned(AtomicReference<InboundTransferHook.PlannedInboundTransferContext> sink) {
        return new InboundTransferHook() {
            @Override public void onPlannedInboundTransfer(String ik, PlannedInboundTransferContext ctx) { sink.set(ctx); }
            @Override public void onInboundTransfer(String ik, InboundTransferContext ctx) {}
        };
    }

    private static Execution transferExecution(String planId, int seq, String srcAssetId, String dstAssetId) {
        TransferInstruction t = new TransferInstruction();
        t.setSource(account("src-fin", srcAssetId));
        t.setDestination(account("dst-fin", dstAssetId));
        t.setAmount("5");
        return wrap(planId, seq, null, opOf(t));
    }

    private static Execution releaseExecution(String planId, int seq, String srcAssetId, String dstAssetId) {
        ReleaseInstruction r = new ReleaseInstruction();
        r.setSource(account("src-fin", srcAssetId));
        r.setDestination(account("dst-fin", dstAssetId));
        r.setAmount("1");
        return wrap(planId, seq, null, opOf(r));
    }

    private static Execution transferExecutionAssignedTo(String planId, int seq, String orgAssigned,
                                                         String srcAssetId, String dstAssetId) {
        TransferInstruction t = new TransferInstruction();
        t.setSource(account("src-fin", srcAssetId));
        t.setDestination(account("dst-fin", dstAssetId));
        t.setAmount("5");
        return wrap(planId, seq, orgAssigned, opOf(t));
    }

    private static ExecutionPlanOperation opOf(Object instruction) {
        ExecutionPlanOperation op = new ExecutionPlanOperation();
        op.setActualInstance(instruction);
        return op;
    }

    private static Execution wrap(String planId, int seq, String orgAssigned, ExecutionPlanOperation op) {
        ExecutionInstruction instr = new ExecutionInstruction();
        instr.setSequence(seq);
        if (orgAssigned != null) {
            instr.setOrganizations(Collections.singletonList(orgAssigned));
        }
        instr.setExecutionPlanOperation(op);

        ExecutionPlan plan = new ExecutionPlan();
        plan.setId(planId);
        plan.setInstructions(Collections.singletonList(instr));

        Execution exec = new Execution();
        exec.setPlan(plan);
        return exec;
    }

    private static LedgerAccountAsset account(String finId, String assetId) {
        FinIdAccount1 fin = new FinIdAccount1();
        fin.setFinId(finId);
        Finp2pAsset asset = new Finp2pAsset();
        asset.setId(assetId);
        Finp2pAssetAccount acct = new Finp2pAssetAccount();
        acct.setAccount(fin);
        acct.setAsset(asset);
        LedgerAccountAsset lea = new LedgerAccountAsset();
        lea.setFinp2pAccount(acct);
        return lea;
    }
}
