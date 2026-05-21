package io.ownera.ledger.adapter.vanilla;

import io.ownera.ledger.adapter.service.CommonService;
import io.ownera.ledger.adapter.service.EscrowService;
import io.ownera.ledger.adapter.service.HealthService;
import io.ownera.ledger.adapter.service.TokenService;
import io.ownera.ledger.adapter.service.mapping.AccountMapping;
import io.ownera.ledger.adapter.service.mapping.AccountMappingService;
import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.AssetBind;
import io.ownera.ledger.adapter.service.model.AssetCreationResult;
import io.ownera.ledger.adapter.service.model.AssetCreationStatus;
import io.ownera.ledger.adapter.service.model.AssetDenomination;
import io.ownera.ledger.adapter.service.model.AssetType;
import io.ownera.ledger.adapter.service.model.Balance;
import io.ownera.ledger.adapter.service.model.Destination;
import io.ownera.ledger.adapter.service.model.ErrorDetails;
import io.ownera.ledger.adapter.service.model.ExecutionContext;
import io.ownera.ledger.adapter.service.model.FailedReceiptStatus;
import io.ownera.ledger.adapter.service.model.FinIdAccount;
import io.ownera.ledger.adapter.service.model.LedgerAssetIdentifier;
import io.ownera.ledger.adapter.service.model.LedgerReference;
import io.ownera.ledger.adapter.service.model.OperationStatus;
import io.ownera.ledger.adapter.service.model.OperationType;
import io.ownera.ledger.adapter.service.model.ProofPolicy;
import io.ownera.ledger.adapter.service.model.Receipt;
import io.ownera.ledger.adapter.service.model.ReceiptOperation;
import io.ownera.ledger.adapter.service.model.Signature;
import io.ownera.ledger.adapter.service.model.Source;
import io.ownera.ledger.adapter.service.model.SuccessReceiptStatus;
import io.ownera.ledger.adapter.service.model.SuccessfulAssetCreation;
import io.ownera.ledger.adapter.service.model.TradeDetails;
import io.ownera.ledger.adapter.service.model.TransactionDetails;
import io.ownera.ledger.adapter.service.plan.InboundTransferHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Reference per-investor ledger implementation, ported from
 * {@code vanilla-service/src/service.ts} in the Node skeleton.
 *
 * <p>Implements the core service interfaces against a {@link LedgerStorage}-backed account
 * model. Adapters that want to extend with on-chain integrations register one or more of
 * {@link AssetDelegate}, {@link TransferDelegate}, {@link EscrowDelegate}; the local DB
 * lock/unlock dance still runs around each delegate call so failures roll back cleanly.
 *
 * <p>{@link AccountMappingService} is delegated to the skeleton's existing implementation
 * (passed in via the constructor) so vanilla doesn't fork a parallel mapping store.
 *
 * <p>PaymentService is intentionally not implemented here; the Node vanilla does the same.
 * Adapters compose vanilla with their own payment service when needed.
 */
public class VanillaServiceImpl implements
        TokenService, EscrowService, CommonService, HealthService,
        AccountMappingService, InboundTransferHook {

    private static final Logger logger = LoggerFactory.getLogger(VanillaServiceImpl.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final LedgerStorage storage;
    private final AccountMappingService accountMappingService;
    private final @Nullable AssetDelegate assetDelegate;
    private final @Nullable TransferDelegate transferDelegate;
    private final @Nullable EscrowDelegate escrowDelegate;

    public VanillaServiceImpl(LedgerStorage storage,
                              AccountMappingService accountMappingService) {
        this(storage, accountMappingService, null, null, null);
    }

    public VanillaServiceImpl(LedgerStorage storage,
                              AccountMappingService accountMappingService,
                              @Nullable AssetDelegate assetDelegate,
                              @Nullable TransferDelegate transferDelegate,
                              @Nullable EscrowDelegate escrowDelegate) {
        this.storage = storage;
        this.accountMappingService = accountMappingService;
        this.assetDelegate = assetDelegate;
        this.transferDelegate = transferDelegate;
        this.escrowDelegate = escrowDelegate;
    }

    // ─── TokenService ───────────────────────────────────────────────────────

    @Override
    public AssetCreationStatus createAsset(String idempotencyKey, Asset asset,
                                           @Nullable AssetBind assetBind, @Nullable Object assetMetadata,
                                           @Nullable String assetName, @Nullable String issuerId,
                                           @Nullable AssetDenomination assetDenomination) {
        logger.info("Creating asset {}", asset.assetId);
        if (assetDelegate != null) {
            AssetCreationResult result = assetDelegate.createAsset(
                    idempotencyKey, asset.assetId, assetBind, assetMetadata,
                    assetName, issuerId, assetDenomination);
            return new SuccessfulAssetCreation(result);
        }
        // No delegate → emit a synthetic CAIP-19 identifier so router-side flows still work
        // for local-only deployments. Matches Node's `db / vanilla` standard for the fallback.
        String tokenId;
        String network;
        String tokenStandard;
        if (assetBind != null && assetBind.tokenIdentifier != null) {
            tokenId = assetBind.tokenIdentifier.tokenId;
            network = "db";
            tokenStandard = "vanilla";
        } else {
            tokenId = generateCid();
            network = "db";
            tokenStandard = "vanilla";
        }
        AssetCreationResult result = new AssetCreationResult(
                tokenId, new LedgerReference(network, "", tokenStandard, null));
        return new SuccessfulAssetCreation(result);
    }

    @Override
    public ReceiptOperation issue(String idempotencyKey, Asset asset, FinIdAccount to,
                                  String amount, @Nullable ExecutionContext exCtx) {
        logger.info("Issuing {} of {} to {}", amount, asset.assetId, to.finId);
        storage.ensureAccount(to.finId, asset.assetId, asset.assetType.name().toLowerCase());
        LedgerTransaction tx = storage.credit(to.finId, amount, asset.assetId,
                details(idempotencyKey, null, "issue", exCtx), asset.assetType.name().toLowerCase());
        Receipt receipt = buildReceipt(tx, OperationType.ISSUE, asset, null,
                new Destination(to.finId, new FinIdAccount(to.finId)), amount, exCtx, null, null);
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public ReceiptOperation transfer(String idempotencyKey, String nonce, Source source,
                                     Destination destination, Asset asset, String quantity,
                                     Signature signature, @Nullable ExecutionContext exCtx) {
        logger.info("Transferring {} of {} from {} to {}", quantity, asset.assetId, source.finId, destination.finId);
        LedgerDetails details = details(idempotencyKey, null, "transfer", exCtx);

        // Local-to-local: a single atomic move in the ledger.
        if (destination.account instanceof FinIdAccount) {
            storage.ensureAccount(destination.finId, asset.assetId, asset.assetType.name().toLowerCase());
            LedgerTransaction tx = storage.move(source.finId, destination.finId, quantity, asset.assetId, details,
                    asset.assetType.name().toLowerCase());
            return new SuccessReceiptStatus(buildReceipt(tx, OperationType.TRANSFER, asset, source, destination,
                    quantity, exCtx, null, null));
        }

        // External destination: lock → outbound transfer → unlockAndDebit on success, unlock on failure.
        if (transferDelegate == null) {
            return new FailedReceiptStatus(new ErrorDetails(1, "External transfer requires a transfer delegate"));
        }
        storage.lock(source.finId, quantity, asset.assetId, details.withIdempotencyKey(idempotencyKey + ":hold"),
                asset.assetType.name().toLowerCase());

        DelegateResult ext = transferDelegate.outboundTransfer(idempotencyKey, source, destination, asset, quantity, exCtx);
        if (!ext.success) {
            storage.unlock(source.finId, quantity, asset.assetId,
                    details.withIdempotencyKey(idempotencyKey + ":unlock"),
                    asset.assetType.name().toLowerCase());
            return new FailedReceiptStatus(new ErrorDetails(1, ext.error != null ? ext.error : "outbound transfer failed"));
        }
        LedgerTransaction tx = storage.unlockAndDebit(source.finId, quantity, asset.assetId,
                details.withIdempotencyKey(idempotencyKey + ":debit"),
                asset.assetType.name().toLowerCase());
        return new SuccessReceiptStatus(buildReceipt(tx, OperationType.TRANSFER, asset, source, destination,
                quantity, exCtx, null, ext.transactionId));
    }

    @Override
    public ReceiptOperation redeem(String idempotencyKey, String nonce, FinIdAccount source, Asset asset,
                                   String quantity, @Nullable String operationId, Signature signature,
                                   @Nullable ExecutionContext exCtx) {
        logger.info("Redeeming {} of {} from {}", quantity, asset.assetId, source.finId);
        LedgerDetails details = details(idempotencyKey, operationId, "redeem", exCtx);
        // If operationId is set, this redeem is finalizing a prior hold (unlock + debit in one
        // atomic step); otherwise it's a straight debit from available funds.
        LedgerTransaction tx = operationId != null
                ? storage.unlockAndDebit(source.finId, quantity, asset.assetId, details, asset.assetType.name().toLowerCase())
                : storage.debit(source.finId, quantity, asset.assetId, details, asset.assetType.name().toLowerCase());
        Receipt receipt = buildReceipt(tx, OperationType.REDEEM, asset,
                new Source(source.finId, new FinIdAccount(source.finId)), null, quantity, exCtx, operationId, null);
        return new SuccessReceiptStatus(receipt);
    }

    @Override
    public String getBalance(Asset asset, String finId) {
        return storage.getBalance(finId, asset.assetId, asset.assetType.name().toLowerCase()).available;
    }

    @Override
    public Balance balance(Asset asset, String finId) {
        LedgerBalance b = storage.getBalance(finId, asset.assetId, asset.assetType.name().toLowerCase());
        return new Balance(b.balance, b.available, b.held);
    }

    // ─── EscrowService ──────────────────────────────────────────────────────

    @Override
    public ReceiptOperation hold(String idempotencyKey, String nonce, Source source,
                                 @Nullable Destination destination, Asset asset,
                                 String quantity, Signature signature, String operationId,
                                 @Nullable ExecutionContext exCtx) {
        logger.info("Hold {} of {} on {}", quantity, asset.assetId, source.finId);
        LedgerDetails details = details(idempotencyKey, operationId, "hold", exCtx);
        LedgerTransaction tx = storage.lock(source.finId, quantity, asset.assetId, details,
                asset.assetType.name().toLowerCase());

        String externalTxId = null;
        if (escrowDelegate != null) {
            DelegateResult ext = escrowDelegate.hold(idempotencyKey, source, destination, asset, quantity, operationId, exCtx);
            if (!ext.success) {
                storage.unlock(source.finId, quantity, asset.assetId,
                        details.withIdempotencyKey(idempotencyKey + ":unlock"),
                        asset.assetType.name().toLowerCase());
                return new FailedReceiptStatus(new ErrorDetails(1, ext.error != null ? ext.error : "external hold failed"));
            }
            externalTxId = ext.transactionId;
        }
        return new SuccessReceiptStatus(buildReceipt(tx, OperationType.HOLD, asset, source, destination,
                quantity, exCtx, operationId, externalTxId));
    }

    @Override
    public ReceiptOperation release(String idempotencyKey, Source source, Destination destination,
                                    Asset asset, String quantity, String operationId,
                                    @Nullable ExecutionContext exCtx) {
        logger.info("Release {} of {} from {} to {}", quantity, asset.assetId, source.finId, destination.finId);
        LedgerDetails details = details(idempotencyKey, operationId, "release", exCtx);

        // Delegate first: if the external release fails, the funds stay held locally so the
        // operator can retry (matches Node's ordering and comment).
        String externalTxId = null;
        if (escrowDelegate != null) {
            DelegateResult ext = escrowDelegate.release(idempotencyKey, source, destination, asset, quantity, operationId, exCtx);
            if (!ext.success) {
                return new FailedReceiptStatus(new ErrorDetails(1, ext.error != null ? ext.error : "external release failed"));
            }
            externalTxId = ext.transactionId;
        }
        storage.ensureAccount(destination.finId, asset.assetId, asset.assetType.name().toLowerCase());
        LedgerTransaction tx = storage.unlockAndMove(source.finId, destination.finId, quantity, asset.assetId, details,
                asset.assetType.name().toLowerCase());
        return new SuccessReceiptStatus(buildReceipt(tx, OperationType.RELEASE, asset, source, destination,
                quantity, exCtx, operationId, externalTxId));
    }

    @Override
    public ReceiptOperation rollback(String idempotencyKey, Source source, Asset asset,
                                     String quantity, String operationId, @Nullable ExecutionContext exCtx) {
        logger.info("Rollback {} of {} on {}", quantity, asset.assetId, source.finId);
        LedgerDetails details = details(idempotencyKey, operationId, "rollback", exCtx);
        String externalTxId = null;
        if (escrowDelegate != null) {
            DelegateResult ext = escrowDelegate.rollback(idempotencyKey, source, asset, quantity, operationId, exCtx);
            if (!ext.success) {
                return new FailedReceiptStatus(new ErrorDetails(1, ext.error != null ? ext.error : "external rollback failed"));
            }
            externalTxId = ext.transactionId;
        }
        LedgerTransaction tx = storage.unlock(source.finId, quantity, asset.assetId, details,
                asset.assetType.name().toLowerCase());
        return new SuccessReceiptStatus(buildReceipt(tx, OperationType.ROLLBACK, asset,
                new Source(source.finId, new FinIdAccount(source.finId)), null, quantity, exCtx, operationId, externalTxId));
    }

    // ─── CommonService ──────────────────────────────────────────────────────

    @Override
    public ReceiptOperation getReceipt(String id) {
        LedgerTransaction tx = storage.getTransaction(id);
        if (tx == null) {
            // Try operationId fallback (matches Node lookup order so callers can use either id).
            tx = storage.findByOperationId(id);
        }
        if (tx == null) {
            return new FailedReceiptStatus(new ErrorDetails(1, "Receipt not found: " + id));
        }
        OperationType opType = parseOperationType(tx.details.operationType);
        Asset asset = new Asset(tx.assetId, parseAssetType(tx.assetType), null);
        Source source = tx.source != null ? new Source(tx.source, new FinIdAccount(tx.source)) : null;
        Destination destination = tx.destination != null ? new Destination(tx.destination, new FinIdAccount(tx.destination)) : null;
        return new SuccessReceiptStatus(buildReceipt(tx, opType, asset, source, destination,
                tx.amount, executionContextFromDetails(tx.details), tx.details.operationId, null));
    }

    @Override
    public OperationStatus operationStatus(String cid) {
        // Vanilla doesn't run its own workflow store — the workflow proxy in the skeleton owns
        // cid-keyed lookups (GET /api/operations/status/{cid} reads from operations.outputs
        // directly). This method exists to satisfy the CommonService contract; calling it
        // bypasses the durable workflow path and is generally a configuration mistake.
        throw new UnsupportedOperationException(
                "operationStatus(cid) is owned by the workflow proxy; poll /api/operations/status/{cid} instead");
    }

    // ─── HealthService ──────────────────────────────────────────────────────

    @Override
    public void liveness() {
        // No external dependencies beyond the ledger DB; a successful storage call proves both
        // jOOQ and the connection pool are healthy.
        storage.getBalance("__health__", "__health__");
    }

    @Override
    public void readiness() {
        liveness();
    }

    // ─── AccountMappingService (delegated to skeleton's impl) ───────────────

    @Override
    public List<AccountMapping> getAccounts(@Nullable List<String> finIds) {
        return accountMappingService.getAccounts(finIds);
    }

    @Override
    public List<AccountMapping> getByFieldValue(String fieldName, String value) {
        return accountMappingService.getByFieldValue(fieldName, value);
    }

    @Override
    public AccountMapping saveAccount(String finId, Map<String, String> fields) {
        return accountMappingService.saveAccount(finId, fields);
    }

    @Override
    public void deleteAccount(String finId, @Nullable String fieldName) {
        accountMappingService.deleteAccount(finId, fieldName);
    }

    // ─── InboundTransferHook ────────────────────────────────────────────────

    @Override
    public void onPlannedInboundTransfer(String idempotencyKey, PlannedInboundTransferContext ctx) {
        // Best-effort: pre-create the destination account so the post-execution credit doesn't
        // race with concurrent ensureAccount on the issue side.
        storage.ensureAccount(ctx.destination, ctx.asset.assetId, ctx.asset.assetType.name().toLowerCase());
    }

    @Override
    public void onInboundTransfer(String idempotencyKey, InboundTransferContext ctx) {
        // Verify via TransferDelegate.onInboundTransfer if one is wired, then credit the local
        // destination. A verification failure skips the credit so an unverifiable transfer
        // can't inflate the destination's balance.
        if (transferDelegate != null) {
            Source source = new Source(ctx.source, new FinIdAccount(ctx.source));
            Destination dest = new Destination(ctx.destination, new FinIdAccount(ctx.destination));
            try {
                transferDelegate.onInboundTransfer(
                        ctx.result != null && ctx.result.transactionId != null ? ctx.result.transactionId : "",
                        source, ctx.asset, dest, ctx.amount, null);
            } catch (InboundTransferVerificationError e) {
                logger.warn("Skipping inbound credit for plan={} dst={} amount={}: {}",
                        ctx.planId, ctx.destination, ctx.amount, e.getMessage());
                return;
            }
        }
        storage.ensureAccount(ctx.destination, ctx.asset.assetId, ctx.asset.assetType.name().toLowerCase());
        storage.credit(ctx.destination, ctx.amount, ctx.asset.assetId,
                details(idempotencyKey, null, "inboundTransfer", null),
                ctx.asset.assetType.name().toLowerCase());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private static LedgerDetails details(String idempotencyKey, @Nullable String operationId,
                                         String operationType, @Nullable ExecutionContext exCtx) {
        LedgerDetails.LedgerExecutionContext lec = exCtx != null
                ? new LedgerDetails.LedgerExecutionContext(exCtx.planId, exCtx.sequence)
                : null;
        return new LedgerDetails(idempotencyKey, operationId, operationType, lec, null);
    }

    private static Receipt buildReceipt(LedgerTransaction tx, OperationType opType, Asset asset,
                                        @Nullable Source source, @Nullable Destination destination,
                                        String quantity, @Nullable ExecutionContext exCtx,
                                        @Nullable String operationId, @Nullable String externalTxId) {
        TransactionDetails txDetails = new TransactionDetails(
                externalTxId != null ? externalTxId : tx.id, operationId);
        TradeDetails tradeDetails = new TradeDetails(exCtx);
        long timestamp = tx.createdAt.toEpochMilli();
        ProofPolicy proof = null;
        return new Receipt(tx.id, opType, asset, source, destination, quantity,
                txDetails, tradeDetails, proof, timestamp);
    }

    private static OperationType parseOperationType(@Nullable String opType) {
        if (opType == null) return OperationType.TRANSFER;
        switch (opType) {
            case "issue":    return OperationType.ISSUE;
            case "transfer": return OperationType.TRANSFER;
            case "redeem":   return OperationType.REDEEM;
            case "hold":     return OperationType.HOLD;
            case "release":  return OperationType.RELEASE;
            case "rollback": return OperationType.ROLLBACK;
            default:         return OperationType.TRANSFER;
        }
    }

    private static AssetType parseAssetType(@Nullable String t) {
        if (t == null) return AssetType.FINP2P;
        try {
            return AssetType.valueOf(t.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AssetType.FINP2P;
        }
    }

    @Nullable
    private static ExecutionContext executionContextFromDetails(LedgerDetails details) {
        return details.executionContext != null
                ? new ExecutionContext(details.executionContext.planId, details.executionContext.sequence)
                : null;
    }

    private static String generateCid() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
