package io.ownera.ledger.adapter.service.model.plan;

import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.FinIdAccount;

public final class TransferInstruction implements ExecutionPlanOperation {
    public final Asset asset;
    public final FinIdAccount source;
    public final FinIdAccount destination;
    public final String amount;

    public TransferInstruction(Asset asset, FinIdAccount source, FinIdAccount destination, String amount) {
        this.asset = asset;
        this.source = source;
        this.destination = destination;
        this.amount = amount;
    }
}
