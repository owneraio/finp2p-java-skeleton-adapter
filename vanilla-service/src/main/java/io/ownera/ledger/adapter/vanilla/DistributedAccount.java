package io.ownera.ledger.adapter.vanilla;

/**
 * Per-investor account snapshot returned by {@link LedgerStorage#listDistributedAccounts}.
 * Used by the distribution flush to iterate every account that currently holds value for
 * a given asset (omnibus excluded).
 */
public final class DistributedAccount {

    public final String finId;
    public final String balance;

    public DistributedAccount(String finId, String balance) {
        this.finId = finId;
        this.balance = balance;
    }
}
