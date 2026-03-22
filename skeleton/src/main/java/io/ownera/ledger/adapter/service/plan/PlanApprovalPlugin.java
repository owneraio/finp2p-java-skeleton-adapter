package io.ownera.ledger.adapter.service.plan;

import io.ownera.ledger.adapter.service.model.*;

import java.util.List;

/**
 * Synchronous plan approval plugin. Adapters implement this to validate
 * plan instructions and return an immediate approval/rejection.
 */
public interface PlanApprovalPlugin {

    PlanApprovalStatus validateIssuance(List<String> organizations, FinIdAccount destination, Asset asset, String amount);

    PlanApprovalStatus validateTransfer(List<String> organizations, FinIdAccount source, DestinationAccount destination, Asset asset, String amount);

    PlanApprovalStatus validateRedemption(List<String> organizations, FinIdAccount source, DestinationAccount destination, Asset asset, String amount);
}
