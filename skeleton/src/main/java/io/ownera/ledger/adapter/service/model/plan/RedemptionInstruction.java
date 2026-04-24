package io.ownera.ledger.adapter.service.model.plan;

import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.FinIdAccount;

import javax.annotation.Nullable;

public final class RedemptionInstruction implements ExecutionPlanOperation {
    public final Asset asset;
    public final FinIdAccount source;
    @Nullable
    public final FinIdAccount destination;
    public final String amount;

    public RedemptionInstruction(Asset asset, FinIdAccount source, @Nullable FinIdAccount destination, String amount) {
        this.asset = asset;
        this.source = source;
        this.destination = destination;
        this.amount = amount;
    }
}
