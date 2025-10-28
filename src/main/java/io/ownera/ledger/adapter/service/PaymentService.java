package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.*;

public interface PaymentService {

    DepositOperation getDepositInstruction(String idempotencyKey, Source owner, Destination destination,
                                           DepositAsset asset, String amount,
                                           Object details, String nonce, Signature signature);

    ReceiptOperation payout(String idempotencyKey, Source source, Destination destination, Asset asset,
                            String quantity, String description, String nonce, Signature signature);

}