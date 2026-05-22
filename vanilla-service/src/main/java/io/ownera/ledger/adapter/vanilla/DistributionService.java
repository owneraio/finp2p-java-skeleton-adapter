package io.ownera.ledger.adapter.vanilla;

import io.ownera.ledger.adapter.service.model.AssetType;

/**
 * Omnibus / distribution operations layered on top of the vanilla per-investor ledger.
 *
 * <p>The omnibus account holds the asset's on-chain balance; per-investor accounts represent
 * sub-allocations of that pool. {@link #distribute} and {@link #reclaim} move value between
 * the omnibus row and an investor row; {@link #syncOmnibus} reconciles the local omnibus row
 * against the actual on-chain balance read via {@link OmnibusDelegate}.
 *
 * <p>Mirrors Node's {@code DistributionService} interface in
 * {@code vanilla-service/src/interfaces.ts}.
 */
public interface DistributionService {

    /**
     * Reads the on-chain omnibus balance via the {@link OmnibusDelegate} and reconciles the
     * local omnibus row. Throws {@link io.ownera.ledger.adapter.service.BusinessException}
     * when the on-chain balance is less than the already-distributed total (the local omnibus
     * row would have to go negative).
     */
    DistributionStatus syncOmnibus(String assetId, AssetType assetType);

    /** Read-only breakdown of omnibus vs distributed value. */
    DistributionStatus getDistributionStatus(String assetId, AssetType assetType);

    /** Allocate omnibus value to an investor account. */
    void distribute(String finId, String assetId, AssetType assetType, String amount);

    /** Return investor value to the undistributed pool. */
    void reclaim(String finId, String assetId, AssetType assetType, String amount);

    /**
     * Reclaim every per-investor account back to omnibus for the given asset. Each reclaim is
     * a separate ledger move (and a separate audit row); the post-flush status is returned.
     */
    DistributionStatus flushDistributions(String assetId, AssetType assetType);
}
