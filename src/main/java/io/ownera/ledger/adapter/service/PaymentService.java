package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.*;

import javax.annotation.Nullable;

public interface PaymentService {

    DepositOperation getDepositInstruction(String idempotencyKey, Source owner, Destination destination,
                                           DepositAsset asset, @Nullable String amount,
                                           @Nullable Object details, @Nullable String nonce, @Nullable Signature signature);

    ReceiptOperation payout(String idempotencyKey, Source source, @Nullable Destination destination, Asset asset,
                            String quantity, @Nullable String description, @Nullable String nonce, @Nullable Signature signature);

}