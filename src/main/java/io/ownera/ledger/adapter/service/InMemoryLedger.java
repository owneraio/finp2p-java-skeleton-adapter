package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Profile("in-memory")
public class InMemoryLedger implements LedgerService {

    private final Map<String, Map<String, Integer>> holdingMap = new HashMap<>();
    private final Map<String, ServiceTokenResult> receiptMap = new HashMap<>();

    @Override
    public ServiceOperationResult<ServiceAssetResult> createAsset(String idempotencyKey, String assetId) throws TokenServiceException {
        holdingMap.put(assetId, new HashMap<>());
        return ServiceOperationResult.completed(new ServiceAssetResult(assetId));
    }

    @Override
    public ServiceOperationResult<ServiceTokenResult> issueAssets(String idempotencyKey, String nonce, String assetId, String destFinId, String amount, ServiceSignature signature) throws TokenServiceException {
        String transactionId = UUID.randomUUID().toString();
        ServiceTokenResult tokenResult = new ServiceTokenResult(transactionId, assetId, null, destFinId, amount);
        receiptMap.put(transactionId, tokenResult);
        holdingMap.get(assetId).put(destFinId, holdingMap.get(assetId).getOrDefault(destFinId, 0) + Integer.parseInt(amount));
        return ServiceOperationResult.completed(tokenResult);
    }

    @Override
    public ServiceOperationResult<ServiceTokenResult> transferAssets(String idempotencyKey, String nonce, String assetId, String sourceFinId, String destFinId, String amount, ServiceSignature signature) throws TokenServiceException {
        String transactionId = UUID.randomUUID().toString();

        ServiceTokenResult tokenResult = new ServiceTokenResult(transactionId, assetId, sourceFinId, destFinId, amount);
        receiptMap.put(transactionId, tokenResult);
        holdingMap.get(assetId).put(sourceFinId, holdingMap.get(assetId).getOrDefault(sourceFinId, 0) - Integer.parseInt(amount));
        holdingMap.get(assetId).put(destFinId, holdingMap.get(assetId).getOrDefault(destFinId, 0) + Integer.parseInt(amount));
        return ServiceOperationResult.completed(tokenResult);
    }

    @Override
    public ServiceOperationResult<ServiceTokenResult> redeemAssets(String idempotencyKey, String nonce, String assetId, String ownerFinId, String amount, ServiceSignature signature) throws TokenServiceException {
        String transactionId = UUID.randomUUID().toString();

        ServiceTokenResult tokenResult = new ServiceTokenResult(transactionId, assetId, ownerFinId, null, amount);
        receiptMap.put(transactionId, tokenResult);
        holdingMap.get(assetId).put(ownerFinId, holdingMap.get(assetId).getOrDefault(ownerFinId, 0) - Integer.parseInt(amount));
        return ServiceOperationResult.completed(tokenResult);
    }

    @Override
    public String getBalance(String assetId, String finId) throws TokenServiceException {
        return holdingMap.get(assetId).getOrDefault(finId, 0).toString();
    }

    @Override
    public ServiceTokenResult getTokenReceipt(String transactionId) throws TokenServiceException {
        ServiceTokenResult result = receiptMap.get(transactionId);
        if (result == null) {
            throw new TokenServiceException("Receipt not found");
        }
        return result;
    }

    @Override
    public ServiceOperationStatus getOperationStatus(String correlationId) throws TokenServiceException {
        return null;
    }
}
