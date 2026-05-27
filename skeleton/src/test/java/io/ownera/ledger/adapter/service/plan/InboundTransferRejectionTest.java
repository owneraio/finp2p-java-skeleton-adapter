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
import io.ownera.finp2p.opapi.model.TransferInstruction;
import io.ownera.ledger.adapter.service.model.ApprovedPlan;
import io.ownera.ledger.adapter.service.model.PlanApprovalStatus;
import io.ownera.ledger.adapter.service.model.RejectedPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link InboundTransferRejection} thrown by an {@link InboundTransferHook} is
 * caught specifically and converted into a {@code RejectedPlan} (carrying the supplied code +
 * message), while any other exception escaping the hook is treated as a hook bug — warn-logged,
 * approval still proceeds. The asymmetry is the whole point of the typed-rejection channel:
 * deliberate reject vs accidental hook failure must produce different outcomes.
 */
class InboundTransferRejectionTest {

    private OperationalSDK sdk;

    @BeforeEach
    void setup() {
        sdk = Mockito.mock(OperationalSDK.class);
    }

    @Test
    void approvePlanRejectsWhenPlannedHookThrowsInboundTransferRejection() throws Exception {
        String planId = "plan-reject-" + System.nanoTime();
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(executionWithTransfer(planId, 1, "org-test"));

        InboundTransferHook hook = new InboundTransferHook() {
            @Override public void onPlannedInboundTransfer(String ik, PlannedInboundTransferContext ctx) {
                throw new InboundTransferRejection(403, "destination not whitelisted");
            }
            @Override public void onInboundTransfer(String ik, InboundTransferContext ctx) {}
        };

        DefaultPlanApprovalService svc = new DefaultPlanApprovalService(
                "org-test", sdk, null, hook);

        PlanApprovalStatus status = svc.approvePlan("ik-" + System.nanoTime(), planId);
        assertTrue(status instanceof RejectedPlan, "InboundTransferRejection must produce RejectedPlan, got " + status.getClass());
        RejectedPlan rejected = (RejectedPlan) status;
        assertEquals(403, rejected.details.code);
        assertEquals("destination not whitelisted", rejected.details.message);
    }

    @Test
    void approvePlanContinuesWhenPlannedHookThrowsOtherException() throws Exception {
        // A hook bug (NullPointerException, IllegalStateException, whatever) must NOT silently
        // fail-close a legitimate plan. It's warn-logged and approval continues.
        String planId = "plan-bug-" + System.nanoTime();
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(executionWithTransfer(planId, 1, "org-test"));

        InboundTransferHook hook = new InboundTransferHook() {
            @Override public void onPlannedInboundTransfer(String ik, PlannedInboundTransferContext ctx) {
                throw new RuntimeException("oops — hook bug");
            }
            @Override public void onInboundTransfer(String ik, InboundTransferContext ctx) {}
        };

        DefaultPlanApprovalService svc = new DefaultPlanApprovalService(
                "org-test", sdk, null, hook);

        PlanApprovalStatus status = svc.approvePlan("ik-" + System.nanoTime(), planId);
        assertTrue(status instanceof ApprovedPlan, "non-typed hook failure must not reject the plan, got " + status.getClass());
    }

    @Test
    void proposeInstructionApprovalRejectsWhenHookThrowsInboundTransferRejection() throws Exception {
        String planId = "plan-instr-reject-" + System.nanoTime();
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(executionWithTransfer(planId, 7, "org-test"));

        InboundTransferHook hook = new InboundTransferHook() {
            @Override public void onPlannedInboundTransfer(String ik, PlannedInboundTransferContext ctx) {}
            @Override public void onInboundTransfer(String ik, InboundTransferContext ctx) {
                throw new InboundTransferRejection(409, "on-chain settlement missing");
            }
        };

        DefaultPlanApprovalService svc = new DefaultPlanApprovalService(
                "org-test", sdk, null, hook);

        PlanApprovalStatus status = svc.proposeInstructionApproval("ik-" + System.nanoTime(), planId, 7);
        assertTrue(status instanceof RejectedPlan, "instruction-level reject must produce RejectedPlan, got " + status.getClass());
        RejectedPlan rejected = (RejectedPlan) status;
        assertEquals(409, rejected.details.code);
        assertEquals("on-chain settlement missing", rejected.details.message);
    }

    @Test
    void proposeInstructionApprovalApprovesWhenHookThrowsOtherException() throws Exception {
        String planId = "plan-instr-bug-" + System.nanoTime();
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(executionWithTransfer(planId, 7, "org-test"));

        InboundTransferHook hook = new InboundTransferHook() {
            @Override public void onPlannedInboundTransfer(String ik, PlannedInboundTransferContext ctx) {}
            @Override public void onInboundTransfer(String ik, InboundTransferContext ctx) {
                throw new IllegalStateException("hook is broken");
            }
        };

        DefaultPlanApprovalService svc = new DefaultPlanApprovalService(
                "org-test", sdk, null, hook);

        PlanApprovalStatus status = svc.proposeInstructionApproval("ik-" + System.nanoTime(), planId, 7);
        assertTrue(status instanceof ApprovedPlan, "non-typed hook failure must not reject the instruction, got " + status.getClass());
    }

    /**
     * Build a minimal Execution with a single TransferInstruction assigned to {@code orgId} at
     * the given {@code sequence}. Source and destination are populated enough for the helpers in
     * DefaultPlanApprovalService to extract a fin-id and asset id without NPE.
     */
    private static Execution executionWithTransfer(String planId, int sequence, String orgId) {
        FinIdAccount1 srcFin = new FinIdAccount1();
        srcFin.setFinId("src-fin-id");
        FinIdAccount1 dstFin = new FinIdAccount1();
        dstFin.setFinId("dst-fin-id");

        // Destination asset must carry the org prefix matching orgId so the inbound hook's
        // destination-asset-org gate fires (the hook only triggers when we own the dest asset).
        Finp2pAsset destAsset = new Finp2pAsset();
        destAsset.setId(orgId + ":102:asset-1");
        Finp2pAsset srcAsset = new Finp2pAsset();
        srcAsset.setId("org-other:102:asset-src");

        Finp2pAssetAccount srcAcct = new Finp2pAssetAccount();
        srcAcct.setAccount(srcFin);
        srcAcct.setAsset(srcAsset);
        Finp2pAssetAccount dstAcct = new Finp2pAssetAccount();
        dstAcct.setAccount(dstFin);
        dstAcct.setAsset(destAsset);

        LedgerAccountAsset src = new LedgerAccountAsset();
        src.setFinp2pAccount(srcAcct);
        LedgerAccountAsset dst = new LedgerAccountAsset();
        dst.setFinp2pAccount(dstAcct);

        TransferInstruction transfer = new TransferInstruction();
        transfer.setSource(src);
        transfer.setDestination(dst);
        transfer.setAmount("100");

        ExecutionPlanOperation op = new ExecutionPlanOperation();
        op.setActualInstance(transfer);

        ExecutionInstruction instr = new ExecutionInstruction();
        instr.setSequence(sequence);
        instr.setOrganizations(Collections.singletonList(orgId));
        instr.setExecutionPlanOperation(op);

        ExecutionPlan plan = new ExecutionPlan();
        plan.setId(planId);
        plan.setInstructions(Collections.singletonList(instr));

        Execution exec = new Execution();
        exec.setPlan(plan);
        return exec;
    }
}
