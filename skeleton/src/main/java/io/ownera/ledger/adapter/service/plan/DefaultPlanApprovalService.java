package io.ownera.ledger.adapter.service.plan;

import io.ownera.finp2p.OperationalSDK;
import io.ownera.finp2p.opapi.ApiException;
import io.ownera.finp2p.opapi.model.AccountInformation;
import io.ownera.finp2p.opapi.model.CryptocurrencyAsset;
import io.ownera.finp2p.opapi.model.Execution;
import io.ownera.finp2p.opapi.model.ExecutionInstruction;
import io.ownera.finp2p.opapi.model.ExecutionPlan;
import io.ownera.finp2p.opapi.model.ExecutionPlanOperation;
import io.ownera.finp2p.opapi.model.FiatAsset;
import io.ownera.finp2p.opapi.model.Finp2pAsset;
import io.ownera.finp2p.opapi.model.HoldInstruction;
import io.ownera.finp2p.opapi.model.IssueInstruction;
import io.ownera.finp2p.opapi.model.RedemptionInstruction;
import io.ownera.finp2p.opapi.model.TransferInstruction;
import io.ownera.ledger.adapter.service.PlanApprovalService;
import io.ownera.ledger.adapter.service.model.*;
import io.ownera.ledger.adapter.service.workflow.CallbackClient;
import io.ownera.ledger.adapter.service.workflow.CorrelationIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.List;

/**
 * Default plan approval service that fetches the execution plan from the FinP2P API,
 * filters instructions for the current org, and delegates validation to a plugin.
 * <p>
 * If no plugin is registered, all plans are auto-approved.
 * Sync plugin returns immediate approval/rejection.
 * Async plugin returns pending with CID; the plugin calls back when done.
 */
public class DefaultPlanApprovalService implements PlanApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPlanApprovalService.class);

    private final String orgId;
    private final @Nullable OperationalSDK finP2PSDK;
    private final @Nullable PlanApprovalPlugin syncPlugin;
    private final @Nullable AsyncPlanApprovalPlugin asyncPlugin;
    private final @Nullable InboundTransferHook inboundTransferHook;
    private final @Nullable CallbackClient callbackClient;

    public DefaultPlanApprovalService(String orgId,
                                      @Nullable OperationalSDK finP2PSDK,
                                      @Nullable PlanApprovalPlugin syncPlugin,
                                      @Nullable AsyncPlanApprovalPlugin asyncPlugin,
                                      @Nullable InboundTransferHook inboundTransferHook,
                                      @Nullable CallbackClient callbackClient) {
        this.orgId = orgId;
        this.finP2PSDK = finP2PSDK;
        this.syncPlugin = syncPlugin;
        this.asyncPlugin = asyncPlugin;
        this.inboundTransferHook = inboundTransferHook;
        this.callbackClient = callbackClient;
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

        return validatePlan(idempotencyKey, planId, execution);
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
                    String destFinId = transfer.getDestination() != null ? transfer.getDestination().getFinId() : null;
                    String srcFinId = transfer.getSource() != null ? transfer.getSource().getFinId() : null;
                    io.ownera.finp2p.opapi.model.Asset a = transfer.getAsset();

                    try {
                        inboundTransferHook.onInboundTransfer(idempotencyKey,
                                new InboundTransferHook.InboundTransferContext(
                                        planId, srcFinId,
                                        toInternalAsset(a),
                                        destFinId, transfer.getAmount(),
                                        instructionSequence, null));
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
    public void proposalStatus(String planId, String status, String requestType) {
        logger.info("Proposal status: plan={}, status={}, type={}", planId, status, requestType);
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
            return validateIssuance(idempotencyKey, organizations,
                    toFinIdAccount(issue.getDestination()),
                    toInternalAsset(issue.getAsset()),
                    issue.getAmount());

        } else if (instruction instanceof TransferInstruction) {
            TransferInstruction transfer = (TransferInstruction) instruction;
            FinIdAccount source = toFinIdAccount(transfer.getSource());
            DestinationAccount dest = toDestinationAccount(transfer.getDestination());
            Asset asset = toInternalAsset(transfer.getAsset());

            // Notify inbound transfer hook if destination is our org
            if (inboundTransferHook != null && transfer.getDestination() != null) {
                try {
                    inboundTransferHook.onPlannedInboundTransfer(idempotencyKey,
                            new InboundTransferHook.PlannedInboundTransferContext(
                                    planId,
                                    source.finId,
                                    asset,
                                    transfer.getDestination().getFinId(),
                                    transfer.getAmount()));
                } catch (Exception e) {
                    logger.warn("Planned inbound transfer hook failed: {}", e.getMessage());
                }
            }

            return validateTransfer(idempotencyKey, organizations, source, dest, asset, transfer.getAmount());

        } else if (instruction instanceof HoldInstruction) {
            HoldInstruction hold = (HoldInstruction) instruction;
            return validateTransfer(idempotencyKey, organizations,
                    toFinIdAccount(hold.getSource()),
                    toDestinationAccount(hold.getDestination()),
                    toInternalAsset(hold.getAsset()),
                    hold.getAmount());

        } else if (instruction instanceof RedemptionInstruction) {
            RedemptionInstruction redeem = (RedemptionInstruction) instruction;
            return validateRedemption(idempotencyKey, organizations,
                    toFinIdAccount(redeem.getSource()),
                    toDestinationAccount(redeem.getDestination()),
                    toInternalAsset(redeem.getAsset()),
                    redeem.getAmount());
        }

        // AwaitInstruction, ReleaseInstruction, RevertHoldInstruction — auto-approve
        return new ApprovedPlan();
    }

    private PlanApprovalStatus validateIssuance(String idempotencyKey, List<String> organizations,
                                                   FinIdAccount destination, Asset asset, String amount) {
        if (asyncPlugin != null) {
            String cid = CorrelationIdGenerator.generate();
            asyncPlugin.validateIssuance(idempotencyKey, cid, organizations, destination, asset, amount);
            return new PendingPlan(cid, new OperationMetadata(new PollingResponseStrategy()));
        }
        if (syncPlugin != null) {
            return syncPlugin.validateIssuance(organizations, destination, asset, amount);
        }
        return new ApprovedPlan();
    }

    private PlanApprovalStatus validateTransfer(String idempotencyKey, List<String> organizations,
                                                    FinIdAccount source, DestinationAccount destination, Asset asset, String amount) {
        if (asyncPlugin != null) {
            String cid = CorrelationIdGenerator.generate();
            asyncPlugin.validateTransfer(idempotencyKey, cid, organizations, source, destination, asset, amount);
            return new PendingPlan(cid, new OperationMetadata(new PollingResponseStrategy()));
        }
        if (syncPlugin != null) {
            return syncPlugin.validateTransfer(organizations, source, destination, asset, amount);
        }
        return new ApprovedPlan();
    }

    private PlanApprovalStatus validateRedemption(String idempotencyKey, List<String> organizations,
                                                      FinIdAccount source, DestinationAccount destination, Asset asset, String amount) {
        if (asyncPlugin != null) {
            String cid = CorrelationIdGenerator.generate();
            asyncPlugin.validateRedemption(idempotencyKey, cid, organizations, source, destination, asset, amount);
            return new PendingPlan(cid, new OperationMetadata(new PollingResponseStrategy()));
        }
        if (syncPlugin != null) {
            return syncPlugin.validateRedemption(organizations, source, destination, asset, amount);
        }
        return new ApprovedPlan();
    }

    // --- Helpers to convert SDK types to internal model ---

    private static FinIdAccount toFinIdAccount(@Nullable AccountInformation account) {
        if (account == null) return new FinIdAccount("");
        return new FinIdAccount(account.getFinId() != null ? account.getFinId() : "");
    }

    private static DestinationAccount toDestinationAccount(@Nullable AccountInformation account) {
        return toFinIdAccount(account);
    }

    private static Asset toInternalAsset(@Nullable io.ownera.finp2p.opapi.model.Asset sdkAsset) {
        if (sdkAsset == null) return new Asset("", AssetType.FINP2P);
        Object actual = sdkAsset.getActualInstance();
        if (actual instanceof Finp2pAsset) {
            Finp2pAsset fa = (Finp2pAsset) actual;
            return new Asset(fa.getResourceId() != null ? fa.getResourceId() : "", AssetType.FINP2P);
        } else if (actual instanceof FiatAsset) {
            return new Asset("", AssetType.FIAT);
        } else if (actual instanceof CryptocurrencyAsset) {
            return new Asset("", AssetType.CRYPTOCURRENCY);
        }
        return new Asset("", AssetType.FINP2P);
    }
}
