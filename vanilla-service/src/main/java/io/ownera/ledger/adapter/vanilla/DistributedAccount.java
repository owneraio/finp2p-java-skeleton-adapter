package io.ownera.ledger.adapter.vanilla;

import java.math.BigDecimal;

/**
 * Per-investor account snapshot returned by {@link LedgerStorage#listDistributedAccounts}.
 * Used by the distribution flush to iterate every account that currently holds value for
 * a given asset (omnibus excluded).
 *
 * <p>{@code balance} is the raw row total; {@code held} is the locked portion; {@code available}
 * = {@code balance − held} is the spendable amount the flush can actually move. The flush
 * uses {@code available} so an account with an outstanding escrow lock doesn't trip the
 * {@code CHECK (held <= balance)} constraint mid-loop.
 */
public final class DistributedAccount {

    public final String finId;
    public final String balance;
    public final String held;
    public final String available;

    public DistributedAccount(String finId, String balance, String held) {
        this.finId = finId;
        this.balance = balance;
        this.held = held;
        this.available = new BigDecimal(balance).subtract(new BigDecimal(held)).toPlainString();
    }
}
