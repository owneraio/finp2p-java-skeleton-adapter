package io.ownera.ledger.adapter.service.plan;

import io.ownera.ledger.adapter.service.model.*;

import java.util.List;

/**
 * Asynchronous plan approval plugin. Adapters implement this when validation
 * requires external calls or delays. The plugin receives a CID and is expected
 * to call back with the result via CallbackClient.sendCallback().
 */
public interface AsyncPlanApprovalPlugin {

    void validateIssuance(String idempotencyKey, String cid, List<String> organizations, FinIdAccount destination, Asset asset, String amount);

    void validateTransfer(String idempotencyKey, String cid, List<String> organizations, FinIdAccount source, DestinationAccount destination, Asset asset, String amount);

    void validateRedemption(String idempotencyKey, String cid, List<String> organizations, FinIdAccount source, DestinationAccount destination, Asset asset, String amount);
}
