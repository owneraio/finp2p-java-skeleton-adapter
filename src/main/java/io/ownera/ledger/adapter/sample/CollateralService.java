package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.PaymentService;
import io.ownera.ledger.adapter.service.model.*;
import org.jetbrains.annotations.Nullable;

public class CollateralService implements PaymentService {
    @Override
    public DepositOperation getDepositInstruction(String idempotencyKey, Source owner, Destination destination,
                                                  DepositAsset asset, @Nullable String amount, @Nullable Object details, @Nullable String nonce, @Nullable Signature signature) {
        return null;
    }

    @Override
    public ReceiptOperation payout(String idempotencyKey, Source source, @Nullable Destination destination, Asset asset,
                                   String quantity, @Nullable String description, @Nullable String nonce, @Nullable Signature signature) {
        return null;
    }
}
