package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.TokenService;
import io.ownera.ledger.adapter.service.TokenServiceException;
import io.ownera.ledger.adapter.service.model.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Profile("in-memory")
public class InMemoryLedger implements TokenService {

    private final Map<String, Map<String, Integer>> holdingMap = new HashMap<>();
    private final Map<String, ServiceTokenResult> receiptMap = new HashMap<>();

    @Override
    public AssetCreationStatus createAsset(String idempotencyKey, Asset asset) throws TokenServiceException {
        holdingMap.put(asset.assetId, new HashMap<>());
        return new SuccessfulAssetCreation(new AssetCreationResult(asset.assetId, null));
    }

    @Override
    public ReceiptOperation issue(String idempotencyKey, Asset asset, FinIdAccount to, String amount, ExecutionContext exCtx) throws TokenServiceException {
                String transactionId = UUID.randomUUID().toString();
        ServiceTokenResult tokenResult = new ServiceTokenResult(transactionId, asset.assetId, null, to.finId, amount);
        receiptMap.put(transactionId, tokenResult);
        holdingMap.get(asset.assetId).put(to.finId, holdingMap.get(asset).getOrDefault(to.finId, 0) + Integer.parseInt(amount));
        return ServiceOperationResult.completed(tokenResult);
    }

    @Override
    public ReceiptOperation transfer(String idempotencyKey, String nonce, Source source, Destination destination, Asset asset, String quantity, Signature signature, ExecutionContext exCtx) throws TokenServiceException {
        return null;
    }

    @Override
    public ReceiptOperation redeem(String idempotencyKey, String nonce, FinIdAccount source, Asset asset, String quantity, String operationId, Signature signature, ExecutionContext exCtx) throws TokenServiceException {
        return null;
    }

    @Override
    public String getBalance(String assetId, String finId) throws TokenServiceException {
        return "";
    }

    @Override
    public Balance balance(String assetId, String finId) throws TokenServiceException {
        return null;
    }




//    @Override
//    public ServiceOperationResult<ServiceTokenResult> transferAssets(String idempotencyKey, String nonce, String assetId, String sourceFinId, String destFinId, String amount, Signature signature) throws TokenServiceException {
//        String transactionId = UUID.randomUUID().toString();
//
//        ServiceTokenResult tokenResult = new ServiceTokenResult(transactionId, assetId, sourceFinId, destFinId, amount);
//        receiptMap.put(transactionId, tokenResult);
//        holdingMap.get(assetId).put(sourceFinId, holdingMap.get(assetId).getOrDefault(sourceFinId, 0) - Integer.parseInt(amount));
//        holdingMap.get(assetId).put(destFinId, holdingMap.get(assetId).getOrDefault(destFinId, 0) + Integer.parseInt(amount));
//        return ServiceOperationResult.completed(tokenResult);
//    }
//
//    @Override
//    public ServiceOperationResult<ServiceTokenResult> redeemAssets(String idempotencyKey, String nonce, String assetId, String ownerFinId, String amount, Signature signature) throws TokenServiceException {
//        String transactionId = UUID.randomUUID().toString();
//
//        ServiceTokenResult tokenResult = new ServiceTokenResult(transactionId, assetId, ownerFinId, null, amount);
//        receiptMap.put(transactionId, tokenResult);
//        holdingMap.get(assetId).put(ownerFinId, holdingMap.get(assetId).getOrDefault(ownerFinId, 0) - Integer.parseInt(amount));
//        return ServiceOperationResult.completed(tokenResult);
//    }
//
//    @Override
//    public String getBalance(String assetId, String finId) throws TokenServiceException {
//        return holdingMap.get(assetId).getOrDefault(finId, 0).toString();
//    }
//
//    @Override
//    public ServiceTokenResult getTokenReceipt(String transactionId) throws TokenServiceException {
//        ServiceTokenResult result = receiptMap.get(transactionId);
//        if (result == null) {
//            throw new TokenServiceException("Receipt not found");
//        }
//        return result;
//    }
//
//    @Override
//    public ServiceOperationStatus getOperationStatus(String correlationId) throws TokenServiceException {
//        return null;
//    }
}
