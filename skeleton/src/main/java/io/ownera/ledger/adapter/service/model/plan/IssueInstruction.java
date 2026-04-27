package io.ownera.ledger.adapter.service.model.plan;

import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.FinIdAccount;

public final class IssueInstruction implements ExecutionPlanOperation {
    public final Asset asset;
    public final FinIdAccount destination;
    public final String amount;

    public IssueInstruction(Asset asset, FinIdAccount destination, String amount) {
        this.asset = asset;
        this.destination = destination;
        this.amount = amount;
    }
}
