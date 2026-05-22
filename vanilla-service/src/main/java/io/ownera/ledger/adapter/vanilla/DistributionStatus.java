package io.ownera.ledger.adapter.vanilla;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.ownera.ledger.adapter.service.model.AssetType;

/**
 * Wire-shape returned by {@link DistributionService} (and the {@code /distribution/*} routes).
 *
 * <p>Mirrors Node's {@code DistributionStatus} type in
 * {@code vanilla-service/src/interfaces.ts}: the omnibus top-line plus the
 * already-distributed and still-available breakdown.
 */
public final class DistributionStatus {

    @JsonProperty("assetId")
    public final String assetId;

    @JsonProperty("assetType")
    public final AssetType assetType;

    @JsonProperty("omnibusBalance")
    public final String omnibusBalance;

    @JsonProperty("distributedBalance")
    public final String distributedBalance;

    @JsonProperty("availableBalance")
    public final String availableBalance;

    @JsonCreator
    public DistributionStatus(
            @JsonProperty("assetId") String assetId,
            @JsonProperty("assetType") AssetType assetType,
            @JsonProperty("omnibusBalance") String omnibusBalance,
            @JsonProperty("distributedBalance") String distributedBalance,
            @JsonProperty("availableBalance") String availableBalance) {
        this.assetId = assetId;
        this.assetType = assetType;
        this.omnibusBalance = omnibusBalance;
        this.distributedBalance = distributedBalance;
        this.availableBalance = availableBalance;
    }
}
