package io.ownera.ledger.adapter.service.plan;

import io.ownera.finp2p.OperationalSDK;
import io.ownera.finp2p.opapi.model.Execution;
import io.ownera.finp2p.opapi.model.ExecutionInstruction;
import io.ownera.finp2p.opapi.model.ExecutionPlan;
import io.ownera.finp2p.opapi.model.ExecutionPlanOperation;
import io.ownera.finp2p.opapi.model.FinIdAccount1;
import io.ownera.finp2p.opapi.model.Finp2pAsset;
import io.ownera.finp2p.opapi.model.Finp2pAssetAccount;
import io.ownera.finp2p.opapi.model.InstructionCompletionError;
import io.ownera.finp2p.opapi.model.InstructionCompletionEvent;
import io.ownera.finp2p.opapi.model.InstructionCompletionEventOutput;
import io.ownera.finp2p.opapi.model.LedgerAccountAsset;
import io.ownera.finp2p.opapi.model.ReceiptOutput;
import io.ownera.finp2p.opapi.model.TransferInstruction;
import io.ownera.ledger.adapter.service.model.PlanApprovalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two reports from the field, addressed together:
 *
 * <ol>
 *   <li>{@code ctx.planId} sometimes carried what looked like a nonce rather than the canonical
 *       plan id the adapter could feed into {@code getReceipt} / metadata lookups. The skeleton
 *       now prefers {@code execution.getPlan().getId()} (the fetched plan's own id) over the
 *       wire {@code executionPlan.id}, so the hook always sees the router-canonical handle.</li>
 *   <li>The hook context's {@code result} field was always {@code null} — adapters had to do a
 *       separate {@code getReceipt} round-trip to get the transaction id of the completed
 *       instruction. We now extract the matching entry from
 *       {@code execution.instructionsCompletionEvents}, surface it as {@code ctx.result} and
 *       attach the richer {@code ctx.receipt} (transaction id, operation type, source /
 *       destination fin-ids, quantity).</li>
 * </ol>
 */
class InboundTransferReceiptTest {

    private OperationalSDK sdk;

    @BeforeEach
    void setup() {
        sdk = Mockito.mock(OperationalSDK.class);
    }

    @Test
    void hookContextCarriesCanonicalPlanIdFromFetchedExecution() throws Exception {
        // The router posts /api/plan/proposal with `executionPlan.id` = wireId. We fetch the
        // plan via getExecutionPlan(wireId); the response's plan.id is what the router
        // considers canonical. The hook must see the canonical id, not the wire-envelope id.
        String wireId = "af2eaceb1b31376a05aaa3934404122ea732a30d00000000000000006a15d88c";
        String canonicalId = "plan-canonical-" + System.nanoTime();

        Execution exec = executionWithTransfer(canonicalId, 7, "org-test", null);
        Mockito.when(sdk.getExecutionPlan(wireId)).thenReturn(exec);

        AtomicReference<InboundTransferHook.InboundTransferContext> seen = new AtomicReference<>();
        InboundTransferHook hook = capture(seen);

        DefaultPlanApprovalService svc = new DefaultPlanApprovalService(
                "org-test", sdk, null, hook);

        PlanApprovalStatus status = svc.proposeInstructionApproval("ik-" + System.nanoTime(), wireId, 7);
        assertTrue(status.getClass().getSimpleName().equals("ApprovedPlan"));
        assertNotNull(seen.get(), "hook must fire on transfer instruction");
        assertEquals(canonicalId, seen.get().planId,
                "hook context must carry execution.plan.id, not the wire envelope id");
    }

    @Test
    void hookContextFallsBackToWireIdWhenPlanIdIsAbsent() throws Exception {
        // Defensive: if the fetched plan has a null/empty id, we must not surface a null planId
        // — fall back to the wire id so the hook still has *some* handle to log/cross-reference.
        String wireId = "wire-fallback-" + System.nanoTime();
        Execution exec = executionWithTransfer(null, 7, "org-test", null);
        Mockito.when(sdk.getExecutionPlan(wireId)).thenReturn(exec);

        AtomicReference<InboundTransferHook.InboundTransferContext> seen = new AtomicReference<>();
        DefaultPlanApprovalService svc = new DefaultPlanApprovalService(
                "org-test", sdk, null, capture(seen));

        svc.proposeInstructionApproval("ik", wireId, 7);
        assertNotNull(seen.get());
        assertEquals(wireId, seen.get().planId);
    }

    @Test
    void hookReceivesReceiptDetailsWhenCompletionEventIsAvailable() throws Exception {
        // The router has reported a ReceiptOutput for instruction 7. The hook must see both
        // the lightweight `result.transactionId` summary and the richer `receipt` payload
        // (operation type, source/destination fin-ids, quantity).
        String planId = "plan-with-receipt-" + System.nanoTime();
        ReceiptOutput receipt = new ReceiptOutput();
        receipt.setId("tx-" + System.nanoTime());
        receipt.setOperationType(ReceiptOutput.OperationTypeEnum.ISSUE);
        receipt.setQuantity("42");
        Finp2pAssetAccount source = finp2pAccount("src-fin-id");
        Finp2pAssetAccount destination = finp2pAccount("dst-fin-id");
        receipt.setSource(source);
        receipt.setDestination(destination);

        Execution exec = executionWithTransfer(planId, 7, "org-test", receipt);
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(exec);

        AtomicReference<InboundTransferHook.InboundTransferContext> seen = new AtomicReference<>();
        DefaultPlanApprovalService svc = new DefaultPlanApprovalService(
                "org-test", sdk, null, capture(seen));

        svc.proposeInstructionApproval("ik", planId, 7);

        InboundTransferHook.InboundTransferContext ctx = seen.get();
        assertNotNull(ctx);
        assertNotNull(ctx.result, "lightweight result summary must be populated when a completion event lands");
        assertEquals(InboundTransferHook.InstructionResult.Type.RECEIPT, ctx.result.type);
        assertEquals(receipt.getId(), ctx.result.transactionId);

        assertNotNull(ctx.receipt, "full InstructionReceipt must be attached when ReceiptOutput is available");
        assertEquals(receipt.getId(), ctx.receipt.transactionId);
        assertEquals("issue", ctx.receipt.operationType);
        assertEquals("42", ctx.receipt.quantity);
        assertEquals("src-fin-id", ctx.receipt.sourceFinId);
        assertEquals("dst-fin-id", ctx.receipt.destinationFinId);
    }

    @Test
    void hookReceivesNullReceiptWhenNoCompletionEventForInstruction() throws Exception {
        // Proposal arrived ahead of the completion event landing on the router's
        // instructionsCompletionEvents — the hook must still fire (the adapter may want to
        // observe the planned transfer), but with null result / receipt.
        String planId = "plan-no-event-" + System.nanoTime();
        Execution exec = executionWithTransfer(planId, 7, "org-test", null);
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(exec);

        AtomicReference<InboundTransferHook.InboundTransferContext> seen = new AtomicReference<>();
        DefaultPlanApprovalService svc = new DefaultPlanApprovalService(
                "org-test", sdk, null, capture(seen));

        svc.proposeInstructionApproval("ik", planId, 7);
        InboundTransferHook.InboundTransferContext ctx = seen.get();
        assertNotNull(ctx);
        assertNull(ctx.result);
        assertNull(ctx.receipt);
    }

    @Test
    void hookReceivesErrorResultWhenCompletionEventIsAnError() throws Exception {
        // The router reported InstructionCompletionError instead of a ReceiptOutput. The hook
        // must see the error in `result` (so it knows the instruction failed) and `receipt`
        // must be null (no router-side receipt exists).
        String planId = "plan-with-error-" + System.nanoTime();

        InstructionCompletionError error = new InstructionCompletionError();
        error.setCode(409);
        error.setMessage("on-chain rejection");

        Execution exec = executionWithTransferErrorEvent(planId, 7, "org-test", error);
        Mockito.when(sdk.getExecutionPlan(planId)).thenReturn(exec);

        AtomicReference<InboundTransferHook.InboundTransferContext> seen = new AtomicReference<>();
        DefaultPlanApprovalService svc = new DefaultPlanApprovalService(
                "org-test", sdk, null, capture(seen));

        svc.proposeInstructionApproval("ik", planId, 7);
        InboundTransferHook.InboundTransferContext ctx = seen.get();
        assertNotNull(ctx);
        assertNotNull(ctx.result);
        assertEquals(InboundTransferHook.InstructionResult.Type.ERROR, ctx.result.type);
        assertEquals(409, ctx.result.code);
        assertEquals("on-chain rejection", ctx.result.message);
        assertNull(ctx.receipt, "error completion produces no receipt to surface");
    }

    // ── helpers ────────────────────────────────────────────────────────

    private static InboundTransferHook capture(AtomicReference<InboundTransferHook.InboundTransferContext> sink) {
        return new InboundTransferHook() {
            @Override public void onPlannedInboundTransfer(String ik, PlannedInboundTransferContext ctx) {}
            @Override public void onInboundTransfer(String ik, InboundTransferContext ctx) { sink.set(ctx); }
        };
    }

    private static Finp2pAssetAccount finp2pAccount(String finId) {
        FinIdAccount1 fin = new FinIdAccount1();
        fin.setFinId(finId);
        Finp2pAsset asset = new Finp2pAsset();
        asset.setId("asset-receipt-side");
        Finp2pAssetAccount acct = new Finp2pAssetAccount();
        acct.setAccount(fin);
        acct.setAsset(asset);
        return acct;
    }

    /**
     * Build an Execution with one TransferInstruction at {@code sequence} and (optionally) a
     * matching completion event carrying {@code receipt} on the ReceiptOutput branch.
     */
    private static Execution executionWithTransfer(String planId, int sequence, String orgId, ReceiptOutput receipt) {
        TransferInstruction transfer = new TransferInstruction();
        transfer.setSource(ledgerAccount("src-fin"));
        transfer.setDestination(ledgerAccount("dst-fin"));
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

        if (receipt != null) {
            InstructionCompletionEventOutput output = new InstructionCompletionEventOutput();
            output.setActualInstance(receipt);
            InstructionCompletionEvent event = new InstructionCompletionEvent();
            event.setInstructionSequenceNumber(sequence);
            event.setOutput(output);
            List<InstructionCompletionEvent> events = new ArrayList<>();
            events.add(event);
            exec.setInstructionsCompletionEvents(events);
        }
        return exec;
    }

    private static Execution executionWithTransferErrorEvent(String planId, int sequence, String orgId, InstructionCompletionError error) {
        Execution exec = executionWithTransfer(planId, sequence, orgId, null);

        InstructionCompletionEventOutput output = new InstructionCompletionEventOutput();
        output.setActualInstance(error);
        InstructionCompletionEvent event = new InstructionCompletionEvent();
        event.setInstructionSequenceNumber(sequence);
        event.setOutput(output);

        List<InstructionCompletionEvent> events = new ArrayList<>();
        events.add(event);
        exec.setInstructionsCompletionEvents(events);
        return exec;
    }

    private static LedgerAccountAsset ledgerAccount(String finId) {
        FinIdAccount1 fin = new FinIdAccount1();
        fin.setFinId(finId);
        Finp2pAsset asset = new Finp2pAsset();
        asset.setId("asset-test");
        Finp2pAssetAccount acct = new Finp2pAssetAccount();
        acct.setAccount(fin);
        acct.setAsset(asset);
        LedgerAccountAsset lea = new LedgerAccountAsset();
        lea.setFinp2pAccount(acct);
        return lea;
    }
}
