package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.PaymentService;
import io.ownera.ledger.adapter.service.model.*;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.UUID;

public class CollateralService implements PaymentService {
    @Override
    public DepositOperation getDepositInstruction(String idempotencyKey, Source owner, Destination destination,
                                                  DepositAsset asset, @Nullable String amount, @Nullable Object details, @Nullable String nonce, @Nullable Signature signature) {
        String operationId = UUID.randomUUID().toString();
        return new SuccessfulDepositOperation(
                new DepositInstruction(
                        destination,
                        "Deposit for collateral",
                        Collections.emptyList(),
                        operationId, null)
        );
    }

    @Override
    public ReceiptOperation payout(String idempotencyKey, Source source, @Nullable Destination destination, Asset asset,
                                   String quantity, @Nullable String description, @Nullable String nonce, @Nullable Signature signature) {
        return new FailedReceiptStatus(new ErrorDetails(1, "Not supported"));
    }
}
