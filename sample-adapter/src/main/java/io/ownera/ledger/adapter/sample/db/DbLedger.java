package io.ownera.ledger.adapter.sample.db;

import io.ownera.ledger.adapter.sample.HoldOperation;
import io.ownera.ledger.adapter.sample.Transaction;
import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.service.model.*;
import io.ownera.ledger.adapter.service.proof.ProofProvider;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.UUID;

import static io.ownera.ledger.adapter.db.generated.Tables.TRANSACTIONS;

public class DbLedger implements TokenService, EscrowService, CommonService {

    private static final Logger logger = LoggerFactory.getLogger(DbLedger.class);

    private final DSLContext dsl;
    private final DbStorage storage;
    private final @Nullable ProofProvider proofProvider;

    public DbLedger(DSLContext dsl, @Nullable ProofProvider proofProvider) {
        this.dsl = dsl;
        this.storage = new DbStorage(dsl);
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
        storage.credit(to.finId, amount, asset);
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
        storage.move(source.finId, destination.finId, quantity, asset);
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
        storage.debit(source.finId, quantity, asset);
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
        storage.debit(source.finId, quantity, asset);
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
        storage.credit(destination.finId, quantity, asset);
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
        storage.credit(hold.finId, quantity, asset);
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
        return storage.getBalance(finId, asset);
    }

    @Override
    public Balance balance(Asset asset, String finId) throws TokenServiceException {
        String balance = storage.getBalance(finId, asset);
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
        dsl.insertInto(TRANSACTIONS)
                .set(TRANSACTIONS.ID, tx.id)
                .set(TRANSACTIONS.SOURCE_FIN_ID, sourceFinId)
                .set(TRANSACTIONS.DESTINATION_FIN_ID, destFinId)
                .set(TRANSACTIONS.QUANTITY, tx.quantity)
                .set(TRANSACTIONS.ASSET_ID, tx.asset.assetId)
                .set(TRANSACTIONS.ASSET_TYPE, tx.asset.assetType.name())
                .set(TRANSACTIONS.OPERATION_TYPE, tx.operationType.name())
                .set(TRANSACTIONS.OPERATION_ID, tx.operationId)
                .set(TRANSACTIONS.EXECUTION_PLAN_ID, execPlanId)
                .set(TRANSACTIONS.TIMESTAMP, tx.timestamp)
                .execute();
    }

    private Transaction loadTransaction(String id) {
        return dsl.selectFrom(TRANSACTIONS)
                .where(TRANSACTIONS.ID.eq(id))
                .fetchOptional()
                .map(r -> {
                    String sourceFinId = r.get(TRANSACTIONS.SOURCE_FIN_ID);
                    String destFinId = r.get(TRANSACTIONS.DESTINATION_FIN_ID);
                    String execPlanId = r.get(TRANSACTIONS.EXECUTION_PLAN_ID);
                    FinIdAccount source = sourceFinId != null ? new FinIdAccount(sourceFinId) : null;
                    Destination dest = destFinId != null ? new Destination(destFinId, new FinIdAccount(destFinId)) : null;
                    Asset asset = new Asset(r.get(TRANSACTIONS.ASSET_ID), AssetType.valueOf(r.get(TRANSACTIONS.ASSET_TYPE)));
                    ExecutionContext exCtx = execPlanId != null ? new ExecutionContext(execPlanId, 0) : null;
                    return new Transaction(
                            r.get(TRANSACTIONS.ID), source, dest,
                            r.get(TRANSACTIONS.QUANTITY), asset, exCtx,
                            OperationType.valueOf(r.get(TRANSACTIONS.OPERATION_TYPE)),
                            r.get(TRANSACTIONS.OPERATION_ID),
                            r.get(TRANSACTIONS.TIMESTAMP));
                })
                .orElse(null);
    }
}
