package io.ownera.ledger.adapter.vanilla;

import io.ownera.ledger.adapter.service.BusinessException;
import io.ownera.ledger.adapter.service.model.AssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

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

    public VanillaDistributionService(LedgerStorage storage, OmnibusDelegate omnibusDelegate) {
        if (omnibusDelegate == null) {
            // DistributionService cannot function without an on-chain source of truth — fail
            // fast at construction rather than at the first syncOmnibus call.
            throw new IllegalArgumentException("DistributionService requires a non-null OmnibusDelegate");
        }
        this.storage = storage;
        this.omnibusDelegate = omnibusDelegate;
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
        storage.move(OMNIBUS_FIN_ID, finId, amount, assetId, details, assetTypeStr);
    }

    @Override
    public void reclaim(String finId, String assetId, AssetType assetType, String amount) {
        String assetTypeStr = assetType.name().toLowerCase();
        storage.ensureAccount(OMNIBUS_FIN_ID, assetId, assetTypeStr);
        LedgerDetails details = new LedgerDetails(generateIdempotencyKey(), null, "reclaim", null, null);
        storage.move(finId, OMNIBUS_FIN_ID, amount, assetId, details, assetTypeStr);
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

    private static String generateIdempotencyKey() {
        // Random 16-byte base64url id; uniqueness here is internal — these moves don't have a
        // user-supplied idempotency key, so we generate one per call so the storage layer's
        // dedup index doesn't conflate distinct distributions/reclaims.
        byte[] bytes = new byte[16];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
