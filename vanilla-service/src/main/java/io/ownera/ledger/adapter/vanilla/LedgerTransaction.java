package io.ownera.ledger.adapter.vanilla;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * One row of the {@code transactions} table.
 *
 * <p>Strings for the numeric columns so they round-trip cleanly through the API layer
 * (which deals in string-encoded NUMERICs) without an intermediate {@code BigDecimal}.
 */
public final class LedgerTransaction {

    public final String id;
    public final String assetId;
    public final String assetType;
    public final @Nullable String source;
    public final @Nullable String destination;
    public final String amount;
    public final String sourceHeld;
    public final String destinationHeld;
    public final String action;
    public final LedgerDetails details;
    public final Instant createdAt;

    public LedgerTransaction(String id, String assetId, String assetType,
                             @Nullable String source, @Nullable String destination,
                             String amount, String sourceHeld, String destinationHeld,
                             String action, LedgerDetails details, Instant createdAt) {
        this.id = id;
        this.assetId = assetId;
        this.assetType = assetType;
        this.source = source;
        this.destination = destination;
        this.amount = amount;
        this.sourceHeld = sourceHeld;
        this.destinationHeld = destinationHeld;
        this.action = action;
        this.details = details;
        this.createdAt = createdAt;
    }
}
