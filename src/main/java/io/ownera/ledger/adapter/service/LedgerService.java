package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.*;

public interface LedgerService {

    ServiceOperationResult<ServiceAssetResult> createAsset(String idempotencyKey, String assetId) throws TokenServiceException;

    ServiceOperationResult<ServiceTokenResult> issueAssets(String idempotencyKey, String nonce, String assetId, String destFinId,
                                                           String amount, ServiceSignature signature) throws TokenServiceException;

    ServiceOperationResult<ServiceTokenResult> transferAssets(String idempotencyKey, String nonce, String assetId, String sourceFinId,
                                                              String destFinId, String amount, ServiceSignature signature) throws TokenServiceException;

    ServiceOperationResult<ServiceTokenResult> redeemAssets(String idempotencyKey, String nonce, String assetId,
                                                            String ownerFinId, String amount, ServiceSignature signature) throws TokenServiceException;

    String getBalance(String assetId, String finId) throws TokenServiceException;

    ServiceTokenResult getTokenReceipt(String transactionId) throws TokenServiceException;

    ServiceOperationStatus getOperationStatus(String correlationId) throws TokenServiceException;
}
