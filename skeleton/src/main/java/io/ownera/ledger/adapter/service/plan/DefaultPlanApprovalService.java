package io.ownera.ledger.adapter.service.plan;

import io.ownera.finp2p.OperationalSDK;
import io.ownera.finp2p.opapi.ApiException;
import io.ownera.finp2p.opapi.model.Execution;
import io.ownera.finp2p.opapi.model.ExecutionInstruction;
import io.ownera.finp2p.opapi.model.ExecutionPlan;
import io.ownera.finp2p.opapi.model.ExecutionPlanOperation;
import io.ownera.finp2p.opapi.model.Finp2pAsset;
import io.ownera.finp2p.opapi.model.Finp2pAssetAccount;
import io.ownera.finp2p.opapi.model.HoldInstruction;
import io.ownera.finp2p.opapi.model.InstructionCompletionError;
import io.ownera.finp2p.opapi.model.InstructionCompletionEvent;
import io.ownera.finp2p.opapi.model.InstructionCompletionEventOutput;
import io.ownera.finp2p.opapi.model.IssueInstruction;
import io.ownera.finp2p.opapi.model.LedgerAccountAsset;
import io.ownera.finp2p.opapi.model.ReceiptOutput;
import io.ownera.finp2p.opapi.model.RedemptionInstruction;
import io.ownera.finp2p.opapi.model.ReleaseInstruction;
import io.ownera.finp2p.opapi.model.TransferInstruction;
import io.ownera.ledger.adapter.service.PlanApprovalService;
import io.ownera.ledger.adapter.service.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Default plan approval service that fetches the execution plan from the FinP2P API,
 * filters instructions for the current org, and delegates validation to a plugin.
 * <p>
 * If no plugin is registered, all plans are auto-approved.
 * The plugin returns {@link PlanApprovalStatus} per instruction — {@link ApprovedPlan},
 * {@link RejectedPlan}, or {@link PendingPlan} for deferred decisions (the plugin then calls
 * back via {@code CallbackClient}).
 */
public class DefaultPlanApprovalService implements PlanApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPlanApprovalService.class);

    private final String orgId;
    private final @Nullable OperationalSDK finP2PSDK;
    private final @Nullable PlanApprovalPlugin plugin;
    private final @Nullable InboundTransferHook inboundTransferHook;
    private final @Nullable PlanAnalyzer planAnalyzer;
    private final PlanMetadataRegistry planMetadataRegistry;

    /**
     * Back-compat constructor — no {@link PlanAnalyzer}, default in-memory metadata registry.
     */
    public DefaultPlanApprovalService(String orgId,
                                      @Nullable OperationalSDK finP2PSDK,
                                      @Nullable PlanApprovalPlugin plugin,
                                      @Nullable InboundTransferHook inboundTransferHook) {
        this(orgId, finP2PSDK, plugin, inboundTransferHook, null, new InMemoryPlanMetadataRegistry());
    }

    /**
     * Full constructor. Adapter supplies a {@link PlanAnalyzer} when it wants metadata derived
     * from each approved plan, and (optionally) its own {@link PlanMetadataRegistry} if the
     * default in-memory store isn't durable enough.
     */
    public DefaultPlanApprovalService(String orgId,
                                      @Nullable OperationalSDK finP2PSDK,
                                      @Nullable PlanApprovalPlugin plugin,
                                      @Nullable InboundTransferHook inboundTransferHook,
                                      @Nullable PlanAnalyzer planAnalyzer,
                                      PlanMetadataRegistry planMetadataRegistry) {
        this.orgId = orgId;
        this.finP2PSDK = finP2PSDK;
        this.plugin = plugin;
        this.inboundTransferHook = inboundTransferHook;
        this.planAnalyzer = planAnalyzer;
        this.planMetadataRegistry = planMetadataRegistry;
    }

    /** Expose the registry so adapter-side operation code can look up metadata by planId. */
    public PlanMetadataRegistry getPlanMetadataRegistry() {
        return planMetadataRegistry;
    }

    @Override
    public PlanApprovalStatus approvePlan(String idempotencyKey, String planId) {
        if (finP2PSDK == null) {
            logger.warn("OperationalSDK not configured, auto-approving plan: {}", planId);
            return new ApprovedPlan();
        }

        Execution execution;
        try {
            execution = finP2PSDK.getExecutionPlan(planId);
        } catch (ApiException e) {
            logger.error("Failed to fetch execution plan: {}", planId, e);
            return new RejectedPlan(new ErrorDetails(e.getCode(), "Failed to fetch execution plan: " + e.getMessage()));
        }

        // Stash analyzer-derived metadata in the registry before validation so the registry is
        // populated even if validation later rejects the plan — keeps the metadata available
        // for diagnostic / audit lookups regardless of outcome. Failure inside the analyzer is
        // contained: log and continue so a buggy analyzer can't tank approval.
        runPlanAnalyzer(planId, execution.getPlan());

        return validatePlan(idempotencyKey, planId, execution);
    }

    private void runPlanAnalyzer(String planId, @Nullable ExecutionPlan plan) {
        if (planAnalyzer == null || plan == null) return;
        try {
            Map<String, Object> metadata = planAnalyzer.analyzePlan(plan);
            if (metadata != null) {
                planMetadataRegistry.put(planId, metadata);
            }
        } catch (Exception e) {
            logger.warn("Plan analyzer failed for planId={}: {}", planId, e.getMessage());
        }
    }

    @Override
    public PlanApprovalStatus proposeCancelPlan(String idempotencyKey, String planId) {
        logger.info("Propose cancel plan: {}", planId);
        return new ApprovedPlan();
    }

    @Override
    public PlanApprovalStatus proposeResetPlan(String idempotencyKey, String planId, int proposedSequence) {
        logger.info("Propose reset plan: {} to sequence: {}", planId, proposedSequence);
        return new ApprovedPlan();
    }

    @Override
    public PlanApprovalStatus proposeInstructionApproval(String idempotencyKey, String planId, int instructionSequence) {
        logger.info("Propose instruction approval: plan={}, sequence={}", planId, instructionSequence);

        if (finP2PSDK == null || inboundTransferHook == null) {
            return new ApprovedPlan();
        }

        Execution execution;
        try {
            execution = finP2PSDK.getExecutionPlan(planId);
        } catch (ApiException e) {
            logger.error("Failed to fetch execution plan for instruction approval: {}", planId, e);
            return new RejectedPlan(new ErrorDetails(e.getCode(), "Failed to fetch execution plan: " + e.getMessage()));
        }

        ExecutionPlan plan = execution.getPlan();
        if (plan == null || plan.getInstructions() == null) {
            return new ApprovedPlan();
        }

        // Prefer the plan's own id over the proposal envelope's id. They are usually the same
        // value the router echoes back, but when an adapter sees a 64-char hex in the wire
        // proposal that doesn't match a router-known plan id, falling back to plan.getId()
        // gives the hook the canonical handle for downstream lookups (getReceipt, etc.).
        String canonicalPlanId = plan.getId() != null && !plan.getId().isEmpty()
                ? plan.getId()
                : planId;

        for (ExecutionInstruction instr : plan.getInstructions()) {
            if (instr.getSequence() != null && instr.getSequence() == instructionSequence) {
                ExecutionPlanOperation op = instr.getExecutionPlanOperation();
                if (op == null) continue;
                Object actual = op.getActualInstance();

                // The inbound hook fires for both transfer and release — each moves value to a
                // destination account, and the adapter on the receiving side credits locally.
                LedgerAccountAsset source = null;
                LedgerAccountAsset destination = null;
                String amount = null;
                if (actual instanceof TransferInstruction) {
                    TransferInstruction transfer = (TransferInstruction) actual;
                    source = transfer.getSource();
                    destination = transfer.getDestination();
                    amount = transfer.getAmount();
                } else if (actual instanceof ReleaseInstruction) {
                    ReleaseInstruction release = (ReleaseInstruction) actual;
                    source = release.getSource();
                    destination = release.getDestination();
                    amount = release.getAmount();
                }

                // Gate on the destination asset's org: fire only when *we* are the receiving
                // side. `instr.getOrganizations()` is the executing (sender) org and must not
                // be used here — it would fire the inbound hook on the wrong adapter.
                if (destination != null) {
                    Asset destAsset = toInternalAsset(destination);
                    if (orgId.equals(orgIdFromResource(destAsset.assetId))) {
                        InstructionCompletionEvent event = findCompletionEvent(execution, instructionSequence);
                        InboundTransferHook.InstructionResult result = toInstructionResult(event);
                        InboundTransferHook.InstructionReceipt receipt = toInstructionReceipt(event);

                        try {
                            inboundTransferHook.onInboundTransfer(idempotencyKey,
                                    new InboundTransferHook.InboundTransferContext(
                                            canonicalPlanId, finIdOf(source),
                                            destAsset,
                                            finIdOf(destination), amount,
                                            instructionSequence, result, receipt));
                        } catch (InboundTransferRejection r) {
                            logger.info("Inbound transfer rejected by hook: plan={}, seq={}, code={}, msg={}",
                                    canonicalPlanId, instructionSequence, r.getCode(), r.getMessage());
                            return new RejectedPlan(new ErrorDetails(r.getCode(), r.getMessage()));
                        } catch (Exception e) {
                            logger.warn("Inbound transfer hook failed: {}", e.getMessage());
                        }
                    }
                }
                break;
            }
        }

        return new ApprovedPlan();
    }

    /**
     * Pull the completion event for {@code instructionSequence} out of {@code execution}. The
     * router populates {@code instructionsCompletionEvents} only after the instruction has
     * actually completed on the underlying ledger; returns {@code null} if the event hasn't
     * landed yet (proposal arrived ahead of completion, which is the common race).
     */
    private static @Nullable InstructionCompletionEvent findCompletionEvent(Execution execution, int instructionSequence) {
        if (execution.getInstructionsCompletionEvents() == null) return null;
        for (InstructionCompletionEvent event : execution.getInstructionsCompletionEvents()) {
            if (event.getInstructionSequenceNumber() != null
                    && event.getInstructionSequenceNumber() == instructionSequence) {
                return event;
            }
        }
        return null;
    }

    /**
     * Map the router-side {@code InstructionCompletionEvent} into the framework-native
     * {@link InboundTransferHook.InstructionResult} summary (transaction id + error code +
     * error message). Either branch of the {@code InstructionCompletionEventOutput} oneOf
     * collapses to a flat result here; {@code null} returns surface as "no completion yet".
     */
    private static @Nullable InboundTransferHook.InstructionResult toInstructionResult(@Nullable InstructionCompletionEvent event) {
        if (event == null) return null;
        InstructionCompletionEventOutput output = event.getOutput();
        if (output == null) return null;
        Object actual = output.getActualInstance();
        if (actual instanceof ReceiptOutput) {
            ReceiptOutput receipt = (ReceiptOutput) actual;
            return InboundTransferHook.InstructionResult.receipt(receipt.getId());
        }
        if (actual instanceof InstructionCompletionError) {
            InstructionCompletionError error = (InstructionCompletionError) actual;
            int code = error.getCode() != null ? error.getCode() : 0;
            return InboundTransferHook.InstructionResult.error(code, error.getMessage());
        }
        return null;
    }

    /**
     * Map the receipt branch of a {@code InstructionCompletionEventOutput} into the
     * framework-native {@link InboundTransferHook.InstructionReceipt}. Returns {@code null}
     * for error branches and when no event is attached — adapters use this to look up the
     * full receipt downstream via {@code FinP2PSDK.getReceipt(transactionId)}.
     */
    private static @Nullable InboundTransferHook.InstructionReceipt toInstructionReceipt(@Nullable InstructionCompletionEvent event) {
        if (event == null) return null;
        InstructionCompletionEventOutput output = event.getOutput();
        if (output == null) return null;
        Object actual = output.getActualInstance();
        if (!(actual instanceof ReceiptOutput)) return null;
        ReceiptOutput receipt = (ReceiptOutput) actual;
        String operationType = receipt.getOperationType() != null
                ? receipt.getOperationType().getValue()
                : null;
        String operationId = receipt.getDetails() != null
                && receipt.getDetails().getTransactionDetails() != null
                ? receipt.getDetails().getTransactionDetails().getOperationId()
                : null;
        String sourceFinId = finIdOf(receipt.getSource());
        String destinationFinId = finIdOf(receipt.getDestination());
        return new InboundTransferHook.InstructionReceipt(
                receipt.getId(),
                operationId,
                operationType,
                sourceFinId,
                destinationFinId,
                receipt.getQuantity());
    }

    private static @Nullable String finIdOf(@Nullable Finp2pAssetAccount account) {
        if (account == null || account.getAccount() == null) return null;
        return account.getAccount().getFinId();
    }

    @Override
    public void proposalStatus(String planId, PlanProposal proposal, ProposalStatus status) {
        logger.info("Proposal status: plan={}, status={}, type={}", planId, status, proposal.getClass().getSimpleName());
    }

    private PlanApprovalStatus validatePlan(String idempotencyKey, String planId, Execution execution) {
        ExecutionPlan plan = execution.getPlan();
        if (plan == null || plan.getInstructions() == null) {
            return new ApprovedPlan();
        }

        List<ExecutionInstruction> instructions = plan.getInstructions();

        for (ExecutionInstruction instr : instructions) {
            // Only validate instructions assigned to this org
            if (instr.getOrganizations() == null || !instr.getOrganizations().contains(orgId)) {
                continue;
            }

            ExecutionPlanOperation op = instr.getExecutionPlanOperation();
            if (op == null) continue;

            Object actual = op.getActualInstance();
            List<String> orgs = instr.getOrganizations() != null ? instr.getOrganizations() : Collections.emptyList();
            PlanApprovalStatus result = validateInstruction(idempotencyKey, planId, orgs, actual);
            if (result != null && !(result instanceof ApprovedPlan)) {
                return result;
            }
        }

        return new ApprovedPlan();
    }

    private @Nullable PlanApprovalStatus validateInstruction(String idempotencyKey, String planId,
                                                               List<String> organizations, Object instruction) {
        if (instruction instanceof IssueInstruction) {
            IssueInstruction issue = (IssueInstruction) instruction;
            return validateIssuance(organizations,
                    toFinIdAccount(issue.getDestination()),
                    toInternalAsset(issue.getDestination()),
                    issue.getAmount());

        } else if (instruction instanceof TransferInstruction) {
            TransferInstruction transfer = (TransferInstruction) instruction;

            PlanApprovalStatus rejection = firePlannedInboundHook(idempotencyKey, planId,
                    transfer.getSource(), transfer.getDestination(), transfer.getAmount());
            if (rejection != null) return rejection;

            FinIdAccount source = toFinIdAccount(transfer.getSource());
            DestinationAccount dest = toDestinationAccount(transfer.getDestination());
            Asset asset = toInternalAsset(transfer.getSource() != null ? transfer.getSource() : transfer.getDestination());
            return validateTransfer(organizations, source, dest, asset, transfer.getAmount());

        } else if (instruction instanceof ReleaseInstruction) {
            // A release moves a held position to the destination account — same inbound
            // semantics as transfer: the destination-side adapter credits locally. Release
            // isn't separately plugin-validated (matches Node), so only the hook fires.
            ReleaseInstruction release = (ReleaseInstruction) instruction;
            PlanApprovalStatus rejection = firePlannedInboundHook(idempotencyKey, planId,
                    release.getSource(), release.getDestination(), release.getAmount());
            if (rejection != null) return rejection;
            return new ApprovedPlan();

        } else if (instruction instanceof HoldInstruction) {
            HoldInstruction hold = (HoldInstruction) instruction;
            return validateTransfer(organizations,
                    toFinIdAccount(hold.getSource()),
                    toDestinationAccount(hold.getDestination()),
                    toInternalAsset(hold.getSource() != null ? hold.getSource() : hold.getDestination()),
                    hold.getAmount());

        } else if (instruction instanceof RedemptionInstruction) {
            RedemptionInstruction redeem = (RedemptionInstruction) instruction;
            return validateRedemption(organizations,
                    toFinIdAccount(redeem.getSource()),
                    toDestinationAccount(redeem.getDestination()),
                    toInternalAsset(redeem.getSource() != null ? redeem.getSource() : redeem.getDestination()),
                    redeem.getAmount());
        }

        // AwaitInstruction, RevertHoldInstruction — auto-approve
        return new ApprovedPlan();
    }

    /**
     * Fire {@link InboundTransferHook#onPlannedInboundTransfer} during plan approval, but only
     * when <em>we</em> own the destination asset (i.e. we're the receiving side). The gate is
     * the destination asset's org prefix — NOT the instruction's {@code organizations} list,
     * which carries the executing/sending org and would fire the inbound hook on the wrong
     * adapter.
     *
     * @return a {@link RejectedPlan} if the hook threw {@link InboundTransferRejection};
     *         {@code null} to continue (hook not fired, fired cleanly, or non-typed failure).
     */
    private @Nullable PlanApprovalStatus firePlannedInboundHook(String idempotencyKey, String planId,
                                                                @Nullable LedgerAccountAsset source,
                                                                @Nullable LedgerAccountAsset destination,
                                                                String amount) {
        if (inboundTransferHook == null || destination == null) return null;
        Asset destAsset = toInternalAsset(destination);
        if (!orgId.equals(orgIdFromResource(destAsset.assetId))) return null;
        try {
            inboundTransferHook.onPlannedInboundTransfer(idempotencyKey,
                    new InboundTransferHook.PlannedInboundTransferContext(
                            planId,
                            finIdOf(source),
                            destAsset,
                            finIdOf(destination),
                            amount));
            return null;
        } catch (InboundTransferRejection r) {
            logger.info("Planned inbound transfer rejected by hook: plan={}, code={}, msg={}",
                    planId, r.getCode(), r.getMessage());
            return new RejectedPlan(new ErrorDetails(r.getCode(), r.getMessage()));
        } catch (Exception e) {
            logger.warn("Planned inbound transfer hook failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract the org prefix from a FinP2P resource id of the shape {@code <orgId>:<type>:<rawId>}
     * (e.g. {@code org-b:102:a640edb1-...} → {@code org-b}). Returns {@code null} if the id is
     * null or carries no {@code :} separator.
     */
    static @Nullable String orgIdFromResource(@Nullable String resourceId) {
        if (resourceId == null) return null;
        int i = resourceId.indexOf(':');
        return i > 0 ? resourceId.substring(0, i) : null;
    }

    private PlanApprovalStatus validateIssuance(List<String> organizations,
                                                FinIdAccount destination, Asset asset, String amount) {
        if (plugin != null) {
            return plugin.validateIssuance(organizations, destination, asset, amount);
        }
        return new ApprovedPlan();
    }

    private PlanApprovalStatus validateTransfer(List<String> organizations,
                                                FinIdAccount source, DestinationAccount destination, Asset asset, String amount) {
        if (plugin != null) {
            return plugin.validateTransfer(organizations, source, destination, asset, amount);
        }
        return new ApprovedPlan();
    }

    private PlanApprovalStatus validateRedemption(List<String> organizations,
                                                  FinIdAccount source, DestinationAccount destination, Asset asset, String amount) {
        if (plugin != null) {
            return plugin.validateRedemption(organizations, source, destination, asset, amount);
        }
        return new ApprovedPlan();
    }

    // --- Helpers to convert SDK types to internal model ---
    // 0.28: instructions carry LedgerAccountAsset (asset embedded in account).

    private static FinIdAccount toFinIdAccount(@Nullable LedgerAccountAsset account) {
        if (account == null) return new FinIdAccount("");
        Finp2pAssetAccount finp2p = account.getFinp2pAccount();
        if (finp2p != null && finp2p.getAccount() != null) {
            String finId = finp2p.getAccount().getFinId();
            return new FinIdAccount(finId != null ? finId : "");
        }
        return new FinIdAccount("");
    }

    private static String finIdOf(@Nullable LedgerAccountAsset account) {
        if (account == null) return null;
        Finp2pAssetAccount finp2p = account.getFinp2pAccount();
        if (finp2p != null && finp2p.getAccount() != null) {
            return finp2p.getAccount().getFinId();
        }
        return null;
    }

    private static DestinationAccount toDestinationAccount(@Nullable LedgerAccountAsset account) {
        return toFinIdAccount(account);
    }

    private static Asset toInternalAsset(@Nullable LedgerAccountAsset account) {
        if (account == null) return new Asset("", AssetType.FINP2P);
        Finp2pAssetAccount finp2p = account.getFinp2pAccount();
        if (finp2p != null && finp2p.getAsset() != null) {
            Finp2pAsset asset = finp2p.getAsset();
            return new Asset(asset.getId() != null ? asset.getId() : "", AssetType.FINP2P);
        }
        return new Asset("", AssetType.FINP2P);
    }
}
