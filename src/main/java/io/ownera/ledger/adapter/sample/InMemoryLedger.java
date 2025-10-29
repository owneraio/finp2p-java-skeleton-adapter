package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.service.model.*;
import io.ownera.ledger.adapter.service.proof.ProofProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Profile("in-memory")
public class InMemoryLedger implements TokenService, EscrowService, CommonService {

    private final static Logger logger = LoggerFactory.getLogger(InMemoryLedger.class);

    private final Storage storage = new Storage();
    private final Map<String, Transaction> transactions = new HashMap<>();
    private final @Nullable ProofProvider proofProvider;

    public InMemoryLedger(@Nullable ProofProvider proofProvider) {
        this.proofProvider = proofProvider;
    }

    @Override
    public AssetCreationStatus createAsset(String idempotencyKey, Asset asset,
                                           @Nullable AssetBind assetBind, @Nullable Object assetMetadata,
                                           @Nullable String assetName, @Nullable String issuerId,
                                           @Nullable AssetDenomination assetDenomination, @Nullable AssetIdentifier assetIdentifier) throws TokenServiceException {
        logger.info("Create asset operation: asset={}, assetBind={}, assetMetadata={}, assetName={}, issuerId={}, assetDenomination={}, assetIdentifier={}",
                asset, assetBind, assetMetadata, assetName, issuerId, assetDenomination, assetIdentifier);
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
        logger.info("Issue operation: asset={}, amount={}, to={}, exCtx={}",
                asset, amount, to, exCtx);
        storage.credit(to.finId, amount, asset.assetId);
        Transaction tx = new Transaction(
                UUID.randomUUID().toString(),
                null,
                to.destination(),
                amount,
                asset,
                exCtx,
                OperationType.ISSUE,
                null,
                System.currentTimeMillis()
        );
        registerTransaction(tx);
        Receipt receipt = tx.toReceipt();
        if (proofProvider != null) {
            proofProvider.provideLedgerProof(receipt);
        }
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public ReceiptOperation transfer(String idempotencyKey, String nonce,
                                     Source source, Destination destination, Asset asset, String quantity,
                                     Signature signature, @Nullable ExecutionContext exCtx) throws TokenServiceException {
        logger.info("Transfer operation: asset={}, quantity={}, exCtx={}",
                asset, quantity, exCtx);

        storage.move(source.finId, destination.finId, quantity, asset.assetId);
        Transaction tx = new Transaction(
                UUID.randomUUID().toString(),
                (FinIdAccount) source.account,
                destination,
                quantity,
                asset,
                exCtx,
                OperationType.TRANSFER,
                null,
                System.currentTimeMillis()
        );
        registerTransaction(tx);
        Receipt receipt = tx.toReceipt();
        if (proofProvider != null) {
            proofProvider.provideLedgerProof(receipt);
        }
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public ReceiptOperation redeem(String idempotencyKey, String nonce, FinIdAccount source, Asset asset, String quantity,
                                   @Nullable String operationId, Signature signature, @Nullable ExecutionContext exCtx) throws TokenServiceException {
        logger.info("Redeem operation: asset={}, quantity={}, operationId={}, exCtx={}",
                asset, quantity, operationId, exCtx);
        storage.debit(source.finId, quantity, asset.assetId);
        Transaction tx = new Transaction(
                UUID.randomUUID().toString(),
                source,
                null,
                quantity,
                asset,
                exCtx,
                OperationType.REDEEM,
                null,
                System.currentTimeMillis()
        );
        registerTransaction(tx);
        Receipt receipt = tx.toReceipt();
        if (proofProvider != null) {
            proofProvider.provideLedgerProof(receipt);
        }
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public ReceiptOperation hold(String idempotencyKey, String nonce, Source source, @Nullable Destination destination,
                                 Asset asset, String quantity, Signature signature, String operationId, @Nullable ExecutionContext exCtx) {
        logger.info("Hold operation: asset={}, quantity={}, operationId={}, exCtx={}",
                asset, quantity, operationId, exCtx);
        storage.saveHoldOperation(operationId, source.finId, quantity);
        storage.debit(source.finId, quantity, asset.assetId);
        Transaction tx = new Transaction(
                UUID.randomUUID().toString(),
                (FinIdAccount) source.account,
                destination,
                quantity,
                asset,
                exCtx,
                OperationType.HOLD,
                null,
                System.currentTimeMillis()
        );
        registerTransaction(tx);
        Receipt receipt = tx.toReceipt();
        if (proofProvider != null) {
            proofProvider.provideLedgerProof(receipt);
        }
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public ReceiptOperation release(String idempotencyKey, Source source, Destination destination, Asset asset, String quantity,
                                    String operationId, @Nullable ExecutionContext exCtx) {
        logger.info("Release hold operation: asset={}, quantity={}, operationId={}, exCtx={}",
                asset, quantity, operationId, exCtx);
        HoldOperation hold = this.storage.getHoldOperation(operationId);
        if (hold == null) {
            throw new TokenServiceException("unknown operation: " + operationId);
        }
        if (!source.finId.equals(hold.finId)) {
            throw new TokenServiceException("operation " + operationId + " does not belong to source " + source.finId);
        }
        this.storage.credit(destination.finId, quantity, asset.assetId);
        this.storage.removeHoldOperation(operationId);
        FinIdAccount holdSource = new FinIdAccount(hold.finId);
        Transaction tx = new Transaction(
                UUID.randomUUID().toString(),
                holdSource,
                destination,
                quantity,
                asset,
                exCtx,
                OperationType.RELEASE,
                null,
                System.currentTimeMillis()
        );
        registerTransaction(tx);
        Receipt receipt = tx.toReceipt();
        if (proofProvider != null) {
            proofProvider.provideLedgerProof(receipt);
        }
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public ReceiptOperation rollback(String idempotencyKey, Source source, Asset asset, String quantity, String operationId,
                                     @Nullable ExecutionContext exCtx) {
        logger.info("Rollback hold operation: asset={}, quantity={}, operationId={}, exCtx={}",
                asset, quantity, operationId, exCtx);
        HoldOperation hold = this.storage.getHoldOperation(operationId);
        if (hold == null) {
            throw new TokenServiceException("unknown operation: " + operationId);
        }
        if (!source.finId.equals(hold.finId)) {
            throw new TokenServiceException("operation " + operationId + " does not belong to source " + source.finId);
        }
        this.storage.credit(hold.finId, quantity, asset.assetId);
        this.storage.removeHoldOperation(operationId);
        FinIdAccount holdSource = new FinIdAccount(hold.finId);
        Transaction tx = new Transaction(
                UUID.randomUUID().toString(),
                holdSource,
                null,
                quantity,
                asset,
                exCtx,
                OperationType.RELEASE,
                null,
                System.currentTimeMillis()
        );
        registerTransaction(tx);
        Receipt receipt = tx.toReceipt();
        if (proofProvider != null) {
            proofProvider.provideLedgerProof(receipt);
        }
        return new SuccessReceiptStatus(receipt);
    }


    @Override
    public String getBalance(String assetId, String finId) throws TokenServiceException {
        return storage.getBalance(finId, assetId);
    }

    @Override
    public Balance balance(String assetId, String finId) throws TokenServiceException {
        String balance = storage.getBalance(finId, assetId);
        return new Balance(balance, balance, "0");
    }

    @Override
    public ReceiptOperation getReceipt(String id) {
        Transaction tx = transactions.get(id);
        if (tx == null) {
            throw new TokenServiceException("Transaction not found");
        }
        return new SuccessReceiptStatus(tx.toReceipt());
    }

    @Override
    public OperationStatus operationStatus(String cid) {
        Transaction tx = transactions.get(cid);
        if (tx == null) {
            throw new TokenServiceException("Transaction not found");
        }
        return new SuccessReceiptStatus(tx.toReceipt());
    }



    private void registerTransaction(Transaction tx) {
        this.transactions.put(tx.id, tx);
    }
}
