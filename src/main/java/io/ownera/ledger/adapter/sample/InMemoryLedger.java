package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.service.model.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Profile("in-memory")
public class InMemoryLedger implements TokenService, EscrowService, CommonService, PlanApprovalService {

    private final Map<String, Map<String, Integer>> holdingMap = new HashMap<>();
    private final Map<String, Receipt> recieptMap = new HashMap<>();

    @Override
    public AssetCreationStatus createAsset(String idempotencyKey, Asset asset) throws TokenServiceException {
        holdingMap.put(asset.assetId, new HashMap<>());
        return new SuccessfulAssetCreation(new AssetCreationResult(asset.assetId, null));
    }

    @Override
    public ReceiptOperation issue(String idempotencyKey, Asset asset, FinIdAccount to, String amount, ExecutionContext exCtx) throws TokenServiceException {
        int currentBalance = holdingMap.get(asset.assetId).getOrDefault(to.finId, 0);
        holdingMap.get(asset.assetId).put(to.finId, currentBalance + Integer.parseInt(amount));

        Receipt receipt = Receipt.newIssueReceipt(asset.assetId, to.finId, amount, "", exCtx);
        recieptMap.put(receipt.id, receipt);
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public ReceiptOperation transfer(String idempotencyKey, String nonce,
                                     Source source, Destination destination, Asset asset, String quantity, Signature signature, ExecutionContext exCtx) throws TokenServiceException {
        int currentBalance = holdingMap.get(asset.assetId).getOrDefault(source.finId, 0);
        int amount = Integer.parseInt(quantity);
        holdingMap.get(asset.assetId).put(source.finId, currentBalance - amount);
        holdingMap.get(asset.assetId).put(destination.finId, currentBalance + amount);

        Receipt receipt = Receipt.newTransferReceipt(asset.assetId, source.finId, destination.finId, quantity, "", exCtx);
        recieptMap.put(receipt.id, receipt);
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public ReceiptOperation redeem(String idempotencyKey, String nonce, FinIdAccount source, Asset asset, String quantity, String operationId, Signature signature, ExecutionContext exCtx) throws TokenServiceException {
        int currentBalance = holdingMap.get(asset.assetId).getOrDefault(source.finId, 0);
        int amount = Integer.parseInt(quantity);
        holdingMap.get(asset.assetId).put(source.finId, currentBalance - amount);

        Receipt receipt = Receipt.newRedeemReceipt(asset.assetId, source.finId, quantity, operationId, exCtx);
        recieptMap.put(receipt.id, receipt);
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public String getBalance(String assetId, String finId) throws TokenServiceException {
        int currentBalance = holdingMap.get(assetId).getOrDefault(finId, 0);
        return String.format("%d", currentBalance);
    }

    @Override
    public Balance balance(String assetId, String finId) throws TokenServiceException {
        int currentBalance = holdingMap.get(assetId).getOrDefault(finId, 0);
        String balance = String.format("%d", currentBalance);
        return new Balance(balance, balance, "0");
    }

    @Override
    public ReceiptOperation getReceipt(String id) {
        Receipt receipt = recieptMap.get(id);
        if (receipt != null) {
            return new SuccessReceiptStatus(receipt);
        }
        return null;
    }

    @Override
    public OperationStatus operationStatus(String cid) {
        return null;
    }

    @Override
    public ReceiptOperation hold(String idempotencyKey, String nonce, Source source, Destination destination, io.ownera.finp2p.signing.eip712.models.Asset asset, String quantity, Signature signature, String operationId, ExecutionContext exCtx) {
        return null;
    }

    @Override
    public ReceiptOperation release(String idempotencyKey, Source source, Destination destination, io.ownera.finp2p.signing.eip712.models.Asset asset, String quantity, String operationId, ExecutionContext exCtx) {
        return null;
    }

    @Override
    public ReceiptOperation rollback(String idempotencyKey, Source source, io.ownera.finp2p.signing.eip712.models.Asset asset, String quantity, String operationId, ExecutionContext exCtx) {
        return null;
    }

    @Override
    public PlanApprovalStatus approvePlan(String idempotencyKey, String planId) {
        return new ApprovedPlan();
    }
}
