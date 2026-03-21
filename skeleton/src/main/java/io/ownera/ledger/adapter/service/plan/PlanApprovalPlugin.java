package io.ownera.ledger.adapter.service.plan;

import io.ownera.ledger.adapter.service.model.*;

/**
 * Synchronous plan approval plugin. Adapters implement this to validate
 * plan instructions and return an immediate approval/rejection.
 */
public interface PlanApprovalPlugin {

    PlanApprovalStatus validateIssuance(FinIdAccount destination, Asset asset, String amount);

    PlanApprovalStatus validateTransfer(FinIdAccount source, DestinationAccount destination, Asset asset, String amount);

    PlanApprovalStatus validateRedemption(FinIdAccount source, DestinationAccount destination, Asset asset, String amount);
}
