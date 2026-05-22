package io.ownera.ledger.adapter.vanilla;

/**
 * Storage-layer breakdown of an asset's distribution state.
 *
 * <ul>
 *   <li>{@code omnibusBalance} — top-line balance (omnibus row + per-investor balances).</li>
 *   <li>{@code distributed} — sum of per-investor account balances.</li>
 *   <li>{@code available} — balance still on the omnibus row (not yet distributed).</li>
 * </ul>
 *
 * <p>Numeric strings rather than {@code BigDecimal} so the value round-trips cleanly with the
 * Postgres {@code NUMERIC} column and the API layer (which deals in string-encoded numerics).
 */
public final class DistributionTotals {

    public final String omnibusBalance;
    public final String distributed;
    public final String available;

    public DistributionTotals(String omnibusBalance, String distributed, String available) {
        this.omnibusBalance = omnibusBalance;
        this.distributed = distributed;
        this.available = available;
    }
}
