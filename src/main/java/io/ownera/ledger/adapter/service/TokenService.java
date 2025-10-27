package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.*;

public interface TokenService {

    AssetCreationStatus createAsset(
            String idempotencyKey, Asset asset
    ) throws TokenServiceException;

    ReceiptOperation issue(String idempotencyKey, Asset asset, FinIdAccount to, String amount, ExecutionContext exCtx) throws TokenServiceException;

    ReceiptOperation transfer(String idempotencyKey, String nonce, Source source,
                              Destination destination, Asset asset, String quantity,
                              Signature signature, ExecutionContext exCtx) throws TokenServiceException;

    ReceiptOperation redeem(String idempotencyKey, String nonce, FinIdAccount source, Asset asset,
                            String quantity, String operationId, Signature signature,
                            ExecutionContext exCtx) throws TokenServiceException;

    String getBalance(String assetId, String finId) throws TokenServiceException;

    Balance balance(String assetId, String finId) throws TokenServiceException;


}
