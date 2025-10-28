package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.*;

import javax.annotation.Nullable;

public interface EscrowService {

    ReceiptOperation hold(String idempotencyKey, String nonce, Source source, @Nullable Destination destination, Asset asset,
                          String quantity, Signature signature, String operationId, @Nullable ExecutionContext exCtx);

    ReceiptOperation release(String idempotencyKey, Source source, Destination destination, Asset asset,
                             String quantity, String operationId, @Nullable ExecutionContext exCtx);

    ReceiptOperation rollback(String idempotencyKey, Source source, Asset asset,
                              String quantity, String operationId, @Nullable ExecutionContext exCtx);


}
