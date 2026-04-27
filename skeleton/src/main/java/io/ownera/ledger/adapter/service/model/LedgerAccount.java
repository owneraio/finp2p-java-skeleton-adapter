package io.ownera.ledger.adapter.service.model;

/**
 * Generic ledger account reference with typed discriminator.
 * Aligns with Node.js skeleton's {@code LedgerAccount {type, address}} — flexible alternative
 * to concrete per-ledger wallet classes (e.g. {@link CryptocurrencyWallet}).
 */
public class LedgerAccount implements SourceAccount, DestinationAccount {
    public final String type;
    public final String address;

    public LedgerAccount(String type, String address) {
        this.type = type;
        this.address = address;
    }
}
