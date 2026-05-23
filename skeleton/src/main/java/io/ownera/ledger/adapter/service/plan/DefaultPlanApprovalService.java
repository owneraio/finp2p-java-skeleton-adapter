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
import io.ownera.finp2p.opapi.model.IssueInstruction;
import io.ownera.finp2p.opapi.model.LedgerAccountAsset;
import io.ownera.finp2p.opapi.model.RedemptionInstruction;
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

        for (ExecutionInstruction instr : plan.getInstructions()) {
            if (instr.getSequence() != null && instr.getSequence() == instructionSequence) {
                ExecutionPlanOperation op = instr.getExecutionPlanOperation();
                if (op == null) continue;
                Object actual = op.getActualInstance();

                if (actual instanceof TransferInstruction) {
                    TransferInstruction transfer = (TransferInstruction) actual;
                    String destFinId = finIdOf(transfer.getDestination());
                    String srcFinId = finIdOf(transfer.getSource());
                    Asset asset = toInternalAsset(transfer.getDestination() != null
                            ? transfer.getDestination()
                            : transfer.getSource());

                    try {
                        inboundTransferHook.onInboundTransfer(idempotencyKey,
                                new InboundTransferHook.InboundTransferContext(
                                        planId, srcFinId,
                                        asset,
                                        destFinId, transfer.getAmount(),
                                        instructionSequence, null));
                    } catch (InboundTransferRejection r) {
                        logger.info("Inbound transfer rejected by hook: plan={}, seq={}, code={}, msg={}",
                                planId, instructionSequence, r.getCode(), r.getMessage());
                        return new RejectedPlan(new ErrorDetails(r.getCode(), r.getMessage()));
                    } catch (Exception e) {
                        logger.warn("Inbound transfer hook failed: {}", e.getMessage());
                    }
                }
                break;
            }
        }

        return new ApprovedPlan();
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
            FinIdAccount source = toFinIdAccount(transfer.getSource());
            DestinationAccount dest = toDestinationAccount(transfer.getDestination());
            Asset asset = toInternalAsset(transfer.getSource() != null ? transfer.getSource() : transfer.getDestination());

            // Notify inbound transfer hook if destination is our org
            if (inboundTransferHook != null && transfer.getDestination() != null) {
                try {
                    inboundTransferHook.onPlannedInboundTransfer(idempotencyKey,
                            new InboundTransferHook.PlannedInboundTransferContext(
                                    planId,
                                    source.finId,
                                    asset,
                                    finIdOf(transfer.getDestination()),
                                    transfer.getAmount()));
                } catch (InboundTransferRejection r) {
                    logger.info("Planned inbound transfer rejected by hook: plan={}, code={}, msg={}",
                            planId, r.getCode(), r.getMessage());
                    return new RejectedPlan(new ErrorDetails(r.getCode(), r.getMessage()));
                } catch (Exception e) {
                    logger.warn("Planned inbound transfer hook failed: {}", e.getMessage());
                }
            }

            return validateTransfer(organizations, source, dest, asset, transfer.getAmount());

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

        // AwaitInstruction, ReleaseInstruction, RevertHoldInstruction — auto-approve
        return new ApprovedPlan();
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
