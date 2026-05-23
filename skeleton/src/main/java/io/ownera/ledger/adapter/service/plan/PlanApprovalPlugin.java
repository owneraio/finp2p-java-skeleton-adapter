package io.ownera.ledger.adapter.service.plan;

import io.ownera.ledger.adapter.service.model.*;
import io.ownera.ledger.adapter.service.workflow.CallbackClient;
import io.ownera.ledger.adapter.service.workflow.CorrelationIdGenerator;

import java.util.List;

/**
 * Plan-approval plugin. Adapters implement this to validate plan instructions and return a
 * {@link PlanApprovalStatus} per instruction. A single interface covers both immediate and
 * deferred decisions — the response type carries the mode:
 *
 * <ul>
 *   <li>{@link ApprovedPlan} — instruction is accepted.</li>
 *   <li>{@link RejectedPlan} — instruction is rejected with an {@link ErrorDetails}.</li>
 *   <li>{@link PendingPlan} — decision deferred; the plugin must later call back with the final
 *       outcome via {@link CallbackClient#sendOperationResult(String, OperationStatus)} using
 *       the same correlation id it returned here. Generate the id with
 *       {@link CorrelationIdGenerator#generate()}.</li>
 * </ul>
 *
 * <p>Matches the single-plugin shape used by the Node.js skeleton — Java previously split this
 * into separate sync / async interfaces; that distinction was removed when collapsing toward
 * Node-side parity.
 */
public interface PlanApprovalPlugin {

    PlanApprovalStatus validateIssuance(List<String> organizations, FinIdAccount destination, Asset asset, String amount);

    PlanApprovalStatus validateTransfer(List<String> organizations, FinIdAccount source, DestinationAccount destination, Asset asset, String amount);

    PlanApprovalStatus validateRedemption(List<String> organizations, FinIdAccount source, DestinationAccount destination, Asset asset, String amount);
}
