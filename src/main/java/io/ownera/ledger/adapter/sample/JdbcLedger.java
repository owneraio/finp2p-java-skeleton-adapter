package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.service.model.*;
import io.ownera.ledger.adapter.service.proof.ProofProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class JdbcLedger implements TokenService, EscrowService, CommonService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcLedger.class);

    private final JdbcTemplate jdbc;
    private final JdbcStorage storage;
    private final @Nullable ProofProvider proofProvider;

    public JdbcLedger(JdbcTemplate jdbc, @Nullable ProofProvider proofProvider) {
        this.jdbc = jdbc;
        this.storage = new JdbcStorage(jdbc);
        this.proofProvider = proofProvider;
    }

    @Override
    public AssetCreationStatus createAsset(String idempotencyKey, Asset asset,
                                           @Nullable AssetBind assetBind, @Nullable Object assetMetadata,
                                           @Nullable String assetName, @Nullable String issuerId,
                                           @Nullable AssetDenomination assetDenomination, @Nullable AssetIdentifier assetIdentifier) throws TokenServiceException {
        logger.info("Create asset operation: asset={}", asset);
        String tokenId;
        if (assetBind == null || assetBind.tokenIdentifier == null) {
            tokenId = UUID.randomUUID().toString();
            storage.createAsset(asset.assetId, asset);
        } else {
            tokenId = assetBind.tokenIdentifier.tokenId;
        }
        return new SuccessfulAssetCreation(new AssetCreationResult(tokenId, null));
    }

    @Override
    public ReceiptOperation issue(String idempotencyKey, Asset asset, FinIdAccount to, String amount, @Nullable ExecutionContext exCtx) throws TokenServiceException {
        logger.info("Issue operation: asset={}, amount={}, to={}", asset, amount, to);
        storage.credit(to.finId, amount, asset.assetId);
        Transaction tx = new Transaction(
                UUID.randomUUID().toString(), null, to.destination(),
                amount, asset, exCtx, OperationType.ISSUE, null, System.currentTimeMillis());
        saveTransaction(tx);
        Receipt receipt = tx.toReceipt();
        if (proofProvider != null) proofProvider.provideLedgerProof(receipt);
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public ReceiptOperation transfer(String idempotencyKey, String nonce,
                                     Source source, Destination destination, Asset asset, String quantity,
                                     Signature signature, @Nullable ExecutionContext exCtx) throws TokenServiceException {
        logger.info("Transfer operation: asset={}, quantity={}", asset, quantity);
        storage.move(source.finId, destination.finId, quantity, asset.assetId);
        Transaction tx = new Transaction(
                UUID.randomUUID().toString(), (FinIdAccount) source.account, destination,
                quantity, asset, exCtx, OperationType.TRANSFER, null, System.currentTimeMillis());
        saveTransaction(tx);
        Receipt receipt = tx.toReceipt();
        if (proofProvider != null) proofProvider.provideLedgerProof(receipt);
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public ReceiptOperation redeem(String idempotencyKey, String nonce, FinIdAccount source, Asset asset, String quantity,
                                   @Nullable String operationId, Signature signature, @Nullable ExecutionContext exCtx) throws TokenServiceException {
        logger.info("Redeem operation: asset={}, quantity={}, operationId={}", asset, quantity, operationId);
        storage.debit(source.finId, quantity, asset.assetId);
        Transaction tx = new Transaction(
                UUID.randomUUID().toString(), source, null,
                quantity, asset, exCtx, OperationType.REDEEM, null, System.currentTimeMillis());
        saveTransaction(tx);
        Receipt receipt = tx.toReceipt();
        if (proofProvider != null) proofProvider.provideLedgerProof(receipt);
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public ReceiptOperation hold(String idempotencyKey, String nonce, Source source, @Nullable Destination destination,
                                 Asset asset, String quantity, Signature signature, String operationId, @Nullable ExecutionContext exCtx) {
        logger.info("Hold operation: asset={}, quantity={}, operationId={}", asset, quantity, operationId);
        storage.saveHoldOperation(operationId, source.finId, quantity);
        storage.debit(source.finId, quantity, asset.assetId);
        Transaction tx = new Transaction(
                UUID.randomUUID().toString(), (FinIdAccount) source.account, destination,
                quantity, asset, exCtx, OperationType.HOLD, null, System.currentTimeMillis());
        saveTransaction(tx);
        Receipt receipt = tx.toReceipt();
        if (proofProvider != null) proofProvider.provideLedgerProof(receipt);
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public ReceiptOperation release(String idempotencyKey, Source source, Destination destination, Asset asset, String quantity,
                                    String operationId, @Nullable ExecutionContext exCtx) {
        logger.info("Release operation: asset={}, quantity={}, operationId={}", asset, quantity, operationId);
        HoldOperation hold = storage.getHoldOperation(operationId);
        if (hold == null) {
            throw new TokenServiceException("unknown operation: " + operationId);
        }
        if (!source.finId.equals(hold.finId)) {
            throw new TokenServiceException("operation " + operationId + " does not belong to source " + source.finId);
        }
        storage.credit(destination.finId, quantity, asset.assetId);
        storage.removeHoldOperation(operationId);
        FinIdAccount holdSource = new FinIdAccount(hold.finId);
        Transaction tx = new Transaction(
                UUID.randomUUID().toString(), holdSource, destination,
                quantity, asset, exCtx, OperationType.RELEASE, null, System.currentTimeMillis());
        saveTransaction(tx);
        Receipt receipt = tx.toReceipt();
        if (proofProvider != null) proofProvider.provideLedgerProof(receipt);
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public ReceiptOperation rollback(String idempotencyKey, Source source, Asset asset, String quantity, String operationId,
                                     @Nullable ExecutionContext exCtx) {
        logger.info("Rollback operation: asset={}, quantity={}, operationId={}", asset, quantity, operationId);
        HoldOperation hold = storage.getHoldOperation(operationId);
        if (hold == null) {
            throw new TokenServiceException("unknown operation: " + operationId);
        }
        if (!source.finId.equals(hold.finId)) {
            throw new TokenServiceException("operation " + operationId + " does not belong to source " + source.finId);
        }
        storage.credit(hold.finId, quantity, asset.assetId);
        storage.removeHoldOperation(operationId);
        FinIdAccount holdSource = new FinIdAccount(hold.finId);
        Transaction tx = new Transaction(
                UUID.randomUUID().toString(), holdSource, null,
                quantity, asset, exCtx, OperationType.RELEASE, null, System.currentTimeMillis());
        saveTransaction(tx);
        Receipt receipt = tx.toReceipt();
        if (proofProvider != null) proofProvider.provideLedgerProof(receipt);
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public String getBalance(Asset asset, String finId) throws TokenServiceException {
        return storage.getBalance(finId, asset.assetId);
    }

    @Override
    public Balance balance(Asset asset, String finId) throws TokenServiceException {
        String balance = storage.getBalance(finId, asset.assetId);
        return new Balance(balance, balance, "0");
    }

    @Override
    public ReceiptOperation getReceipt(String id) {
        Transaction tx = loadTransaction(id);
        if (tx == null) {
            throw new TokenServiceException("Transaction not found");
        }
        return new SuccessReceiptStatus(tx.toReceipt());
    }

    @Override
    public OperationStatus operationStatus(String cid) {
        Transaction tx = loadTransaction(cid);
        if (tx == null) {
            throw new TokenServiceException("Transaction not found");
        }
        return new SuccessReceiptStatus(tx.toReceipt());
    }

    private void saveTransaction(Transaction tx) {
        String sourceFinId = tx.source != null ? tx.source.finId : null;
        String destFinId = tx.destination != null ? tx.destination.finId : null;
        String execPlanId = tx.executionContext != null ? tx.executionContext.planId : null;
        jdbc.update(
                "INSERT INTO transactions (id, source_fin_id, destination_fin_id, quantity, asset_id, asset_type, " +
                        "operation_type, operation_id, execution_plan_id, timestamp) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                tx.id, sourceFinId, destFinId, tx.quantity,
                tx.asset.assetId, tx.asset.assetType.name(),
                tx.operationType.name(), tx.operationId, execPlanId, tx.timestamp);
    }

    private Transaction loadTransaction(String id) {
        List<Transaction> results = jdbc.query(
                "SELECT * FROM transactions WHERE id = ?",
                (rs, rowNum) -> {
                    String sourceFinId = rs.getString("source_fin_id");
                    String destFinId = rs.getString("destination_fin_id");
                    String execPlanId = rs.getString("execution_plan_id");
                    FinIdAccount source = sourceFinId != null ? new FinIdAccount(sourceFinId) : null;
                    Destination dest = destFinId != null ? new Destination(destFinId, new FinIdAccount(destFinId)) : null;
                    Asset asset = new Asset(rs.getString("asset_id"), AssetType.valueOf(rs.getString("asset_type")));
                    ExecutionContext exCtx = execPlanId != null ? new ExecutionContext(execPlanId, 0) : null;
                    return new Transaction(
                            rs.getString("id"), source, dest,
                            rs.getString("quantity"), asset, exCtx,
                            OperationType.valueOf(rs.getString("operation_type")),
                            rs.getString("operation_id"),
                            rs.getLong("timestamp"));
                }, id);
        return results.isEmpty() ? null : results.get(0);
    }
}
