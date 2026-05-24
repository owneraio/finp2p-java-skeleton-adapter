package io.ownera.ledger.adapter.vanilla;

import io.ownera.finp2p.FinP2PSDK;
import io.ownera.finp2p.OperationalSDK;
import io.ownera.finp2p.opapi.model.Finp2pAsset;
import io.ownera.finp2p.opapi.model.ImportTxAccount;
import io.ownera.finp2p.opapi.model.ImportTxAssetAccount;
import io.ownera.finp2p.opapi.model.ImportTxLedgerAssetAccount;
import io.ownera.finp2p.opapi.model.LedgerAssetIdentifierTypeCAIP19;
import io.ownera.finp2p.opapi.model.Transaction;
import io.ownera.finp2p.opapi.model.TransactionDetails;
import io.ownera.finp2p.oss.models.OssAsset;
import io.ownera.ledger.adapter.service.BusinessException;
import io.ownera.ledger.adapter.service.model.AssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * {@link DistributionService} implementation backed by {@link LedgerStorage} and a required
 * {@link OmnibusDelegate}.
 *
 * <p>Ported from Node's vanilla service's distribution methods
 * ({@code vanilla-service/src/service.ts}). The omnibus account is identified by a well-known
 * synthetic finId ({@code __omnibus__}) so distribution operations never collide with real
 * investor finIds.
 */
public class VanillaDistributionService implements DistributionService {

    private static final Logger logger = LoggerFactory.getLogger(VanillaDistributionService.class);
    private static final SecureRandom RNG = new SecureRandom();

    /** Well-known finId prefix used in the {@code accounts} table for omnibus rows. */
    public static final String OMNIBUS_FIN_ID = "__omnibus__";

    private final LedgerStorage storage;
    private final OmnibusDelegate omnibusDelegate;
    private final @Nullable FinP2PSDK finP2PSDK;
    private final @Nullable OperationalSDK operationalSDK;

    /**
     * Back-compat constructor — no router round-trip on distribute/reclaim. Existing wirings
     * keep working; the router's view of who-holds-what won't include omnibus → investor moves
     * done through this service. Adapters that want router visibility wire both SDKs via
     * {@link #VanillaDistributionService(LedgerStorage, OmnibusDelegate, FinP2PSDK, OperationalSDK)}.
     */
    public VanillaDistributionService(LedgerStorage storage, OmnibusDelegate omnibusDelegate) {
        this(storage, omnibusDelegate, null, null);
    }

    /**
     * Full constructor. When both SDK references are supplied, every successful
     * {@link #distribute} and {@link #reclaim} resolves the asset's ledgerIdentifier via the
     * OSS-side {@link FinP2PSDK#getAsset(String)} and posts a matching
     * {@code importTransactions(...)} through the operational-API {@link OperationalSDK} so the
     * FinP2P router stays in sync with these off-router balance moves. Mirrors Node's
     * {@code vanilla-service/src/service.ts} parity. Either SDK left null disables the import
     * (warn-log only); use this when an adapter wants the local bookkeeping without router
     * visibility.
     */
    public VanillaDistributionService(LedgerStorage storage,
                                      OmnibusDelegate omnibusDelegate,
                                      @Nullable FinP2PSDK finP2PSDK,
                                      @Nullable OperationalSDK operationalSDK) {
        if (omnibusDelegate == null) {
            // DistributionService cannot function without an on-chain source of truth — fail
            // fast at construction rather than at the first syncOmnibus call.
            throw new IllegalArgumentException("DistributionService requires a non-null OmnibusDelegate");
        }
        this.storage = storage;
        this.omnibusDelegate = omnibusDelegate;
        this.finP2PSDK = finP2PSDK;
        this.operationalSDK = operationalSDK;
    }

    @Override
    public DistributionStatus syncOmnibus(String assetId, AssetType assetType) {
        String assetTypeStr = assetType.name().toLowerCase();
        // Ensure the omnibus account row exists before we try to reconcile it; without this
        // the UPDATE in syncOmnibusBalance touches zero rows and we'd never spot the divergence.
        storage.ensureAccount(OMNIBUS_FIN_ID, assetId, assetTypeStr);
        String onChainBalance = omnibusDelegate.getOmnibusBalance(assetId, assetType);
        try {
            DistributionTotals totals = storage.syncOmnibusBalance(
                    OMNIBUS_FIN_ID, assetId, onChainBalance, assetTypeStr);
            return new DistributionStatus(assetId, assetType,
                    totals.omnibusBalance, totals.distributed, totals.available);
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            String msg = String.valueOf(e.getMessage());
            // CHECK (balance >= 0) violation — on-chain balance is less than already-distributed
            // total. Translate to a clearer BusinessException matching Node behavior.
            if (msg.contains("accounts_balance_check") || msg.contains("accounts_check")) {
                throw new BusinessException(1,
                        "on-chain balance (" + onChainBalance + ") is less than already distributed");
            }
            throw e;
        }
    }

    @Override
    public DistributionStatus getDistributionStatus(String assetId, AssetType assetType) {
        String assetTypeStr = assetType.name().toLowerCase();
        DistributionTotals totals = storage.getDistributionStatus(OMNIBUS_FIN_ID, assetId, assetTypeStr);
        return new DistributionStatus(assetId, assetType,
                totals.omnibusBalance, totals.distributed, totals.available);
    }

    @Override
    public void distribute(String finId, String assetId, AssetType assetType, String amount) {
        String assetTypeStr = assetType.name().toLowerCase();
        storage.ensureAccount(OMNIBUS_FIN_ID, assetId, assetTypeStr);
        storage.ensureAccount(finId, assetId, assetTypeStr);
        LedgerDetails details = new LedgerDetails(generateIdempotencyKey(), null, "distribute", null, null);
        LedgerTransaction tx = storage.move(OMNIBUS_FIN_ID, finId, amount, assetId, details, assetTypeStr);
        // From the router's view a distribute is an "issue" — the investor newly holds tokens
        // they didn't before, with the omnibus pool as the source.
        importTransaction(Transaction.OperationTypeEnum.ISSUE, finId, assetId, amount, tx.id, tx.createdAt);
    }

    @Override
    public void reclaim(String finId, String assetId, AssetType assetType, String amount) {
        String assetTypeStr = assetType.name().toLowerCase();
        storage.ensureAccount(OMNIBUS_FIN_ID, assetId, assetTypeStr);
        LedgerDetails details = new LedgerDetails(generateIdempotencyKey(), null, "reclaim", null, null);
        LedgerTransaction tx = storage.move(finId, OMNIBUS_FIN_ID, amount, assetId, details, assetTypeStr);
        // From the router's view a reclaim is a "redeem" — the investor's tokens disappear back
        // into the omnibus pool.
        importTransaction(Transaction.OperationTypeEnum.REDEEM, finId, assetId, amount, tx.id, tx.createdAt);
    }

    @Override
    public DistributionStatus flushDistributions(String assetId, AssetType assetType) {
        String assetTypeStr = assetType.name().toLowerCase();
        List<DistributedAccount> accounts = storage.listDistributedAccounts(
                OMNIBUS_FIN_ID, assetId, assetTypeStr);
        logger.info("Flushing {} distributed account(s) for assetId={}", accounts.size(), assetId);
        // Reclaim only the spendable portion (balance − held). An account with an outstanding
        // escrow lock would trip the CHECK (held <= balance) constraint if we tried to move its
        // full balance, and the flush would exit with earlier accounts already drained —
        // leaving the asset in a partially-reclaimed state. Pulling just `available` always
        // succeeds; the held portion stays put until the escrow is released or rolled back,
        // and the caller can re-run flush afterwards. (Shared concern with the Node
        // implementation; tracked there too.)
        for (DistributedAccount account : accounts) {
            if ("0".equals(account.available)) {
                logger.info("Skipping flush of fully-held account finId={} (balance={}, held={})",
                        account.finId, account.balance, account.held);
                continue;
            }
            reclaim(account.finId, assetId, assetType, account.available);
        }
        return getDistributionStatus(assetId, assetType);
    }

    /**
     * Tells the FinP2P router about an off-router balance move via {@code importTransactions}.
     * Mirrors Node's {@code vanilla-service/src/service.ts} private helper:
     *
     * <ol>
     *   <li>Short-circuit if no {@link FinP2PSDK} is wired (back-compat for adapters that
     *       opt out of router round-trips).</li>
     *   <li>Resolve the asset's {@code ledgerIdentifier} via {@code FinP2PSDK.getAsset(assetId)}.
     *       Without it the router has no way to place the transaction on the right ledger, so
     *       skip the import (warn-log) instead of sending a malformed request.</li>
     *   <li>Build a {@link Transaction} with the {@code finId} on the side that matches the
     *       operation type — destination for ISSUE, source for REDEEM — and post it.</li>
     *   <li>Try/catch everything so a router blip never aborts the local move that already
     *       succeeded.</li>
     * </ol>
     */
    private void importTransaction(Transaction.OperationTypeEnum operationType,
                                   String finId, String assetId, String amount,
                                   String txId, Instant createdAt) {
        if (finP2PSDK == null || operationalSDK == null) return;
        try {
            Optional<OssAsset> ossAssetOpt = finP2PSDK.getAsset(assetId);
            if (!ossAssetOpt.isPresent()) {
                logger.warn("Skipping {} import — asset {} not found via FinP2PSDK", operationType, assetId);
                return;
            }
            LedgerAssetIdentifierTypeCAIP19 ledgerIdentifier = toLedgerIdentifier(ossAssetOpt.get());
            if (ledgerIdentifier == null) {
                logger.warn("Skipping {} import — asset {} has no ledgerIdentifier", operationType, assetId);
                return;
            }

            Finp2pAsset asset = new Finp2pAsset();
            asset.setId(assetId);
            asset.setLedgerIdentifier(ledgerIdentifier);

            ImportTxAccount account = new ImportTxAccount();
            account.setFinId(finId);

            ImportTxAssetAccount finp2pAccount = new ImportTxAssetAccount();
            finp2pAccount.setAccount(account);
            finp2pAccount.setAsset(asset);

            ImportTxLedgerAssetAccount side = new ImportTxLedgerAssetAccount();
            side.setFinp2pAccount(finp2pAccount);

            TransactionDetails details = new TransactionDetails();
            details.setTransactionId(txId);

            Transaction tx = new Transaction();
            tx.setId(txId);
            tx.setQuantity(amount);
            tx.setTimestamp(createdAt.getEpochSecond());
            tx.setOperationType(operationType);
            tx.setTransactionDetails(details);
            if (operationType == Transaction.OperationTypeEnum.ISSUE) {
                tx.setDestination(side);
            } else {
                tx.setSource(side);
            }

            operationalSDK.importTransactions(Collections.singletonList(tx));
        } catch (Exception e) {
            // Failure here must not abort the caller — the local DB move already committed and
            // re-throwing would force the caller to roll it back (which we can't actually do
            // after the fact). Log loudly so an operator can reconcile via /distribution/sync or
            // a manual import.
            logger.error("importTransactions failed for {}: finId={}, assetId={}, amount={}, txId={}",
                    operationType, finId, assetId, amount, txId, e);
        }
    }

    /**
     * Pluck the CAIP-19 ledger identifier out of an {@link OssAsset}. Returns {@code null} if
     * the asset isn't bound to a ledger yet (e.g. asset profile only). Mirrors the
     * {@code ossAsset.ledgerAssetInfo.ledgerIdentifier} read in Node's helper, with
     * defensive null checks at every step so a partially-populated GraphQL response can't NPE
     * the import path.
     */
    private static @Nullable LedgerAssetIdentifierTypeCAIP19 toLedgerIdentifier(OssAsset ossAsset) {
        if (ossAsset == null || ossAsset.getLedgerAssetInfo() == null) return null;
        Object id = ossAsset.getLedgerAssetInfo().getLedgerIdentifier();
        if (id == null) return null;
        // OssAsset's ledgerIdentifier is the OSS / GraphQL flavour of the type. The opapi
        // Transaction model needs the opapi flavour. Translate field-by-field; both carry the
        // same (network, tokenId, standard) trio for CAIP-19 bindings.
        LedgerAssetIdentifierTypeCAIP19 out = new LedgerAssetIdentifierTypeCAIP19();
        out.setAssetIdentifierType(LedgerAssetIdentifierTypeCAIP19.AssetIdentifierTypeEnum.CAIP_19);
        out.setNetwork(readString(id, "getNetwork"));
        out.setTokenId(readString(id, "getTokenId"));
        out.setStandard(readString(id, "getStandard"));
        return out;
    }

    private static @Nullable String readString(Object target, String getter) {
        // Reflection rather than a hard type dep on the OSS-side identifier class: the field
        // shape is stable (network/tokenId/standard, CAIP-19), but the concrete class lives in
        // io.ownera.finp2p.oss.models, which we don't want to bind hard from this file (keeps
        // the surface narrow and tolerant if the SDK reshapes the OSS model later).
        try {
            Object value = target.getClass().getMethod(getter).invoke(target);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException e) {
            logger.debug("Could not read {} from {}: {}",
                    getter, target.getClass().getName(), e.getMessage());
            return null;
        }
    }

    private static String generateIdempotencyKey() {
        // Random 16-byte base64url id; uniqueness here is internal — these moves don't have a
        // user-supplied idempotency key, so we generate one per call so the storage layer's
        // dedup index doesn't conflate distinct distributions/reclaims.
        byte[] bytes = new byte[16];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
