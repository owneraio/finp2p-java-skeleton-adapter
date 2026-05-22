package io.ownera.ledger.adapter.vanilla;

/**
 * Balance snapshot for one (finId, asset) pair. Strings, not BigDecimal, so the wire format
 * round-trips cleanly with the {@code NUMERIC} column and downstream API models that hand
 * balances around as strings.
 */
public final class LedgerBalance {

    public final String balance;
    public final String held;
    public final String available;

    public LedgerBalance(String balance, String held, String available) {
        this.balance = balance;
        this.held = held;
        this.available = available;
    }

    public static LedgerBalance zero() {
        return new LedgerBalance("0", "0", "0");
    }
}
