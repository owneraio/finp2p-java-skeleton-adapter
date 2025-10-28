package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.*;

public interface EscrowService {

    ReceiptOperation hold(String idempotencyKey, String nonce, Source source, Destination destination, Asset asset,
                          String quantity, Signature signature, String operationId, ExecutionContext exCtx);

    ReceiptOperation release(String idempotencyKey, Source source, Destination destination, Asset asset,
                             String quantity, String operationId, ExecutionContext exCtx);

    ReceiptOperation rollback(String idempotencyKey, Source source, Asset asset,
                              String quantity, String operationId, ExecutionContext exCtx);


}
