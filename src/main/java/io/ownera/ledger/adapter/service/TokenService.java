package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.*;

import javax.annotation.Nullable;

public interface TokenService {

    AssetCreationStatus createAsset(
            String idempotencyKey, Asset asset,
            @Nullable AssetBind assetBind, @Nullable Object assetMetadata,
            @Nullable String assetName, @Nullable String issuerId,
            @Nullable AssetDenomination assetDenomination, @Nullable AssetIdentifier assetIdentifier

    ) throws TokenServiceException;

    ReceiptOperation issue(String idempotencyKey, Asset asset, FinIdAccount to, String amount, @Nullable ExecutionContext exCtx) throws TokenServiceException;

    ReceiptOperation transfer(String idempotencyKey, String nonce, Source source,
                              Destination destination, Asset asset, String quantity,
                              Signature signature,  @Nullable ExecutionContext exCtx) throws TokenServiceException;

    ReceiptOperation redeem(String idempotencyKey, String nonce, FinIdAccount source, Asset asset,
                            String quantity, @Nullable String operationId, Signature signature,
                            @Nullable ExecutionContext exCtx) throws TokenServiceException;

    String getBalance(String assetId, String finId) throws TokenServiceException;

    Balance balance(String assetId, String finId) throws TokenServiceException;


}
