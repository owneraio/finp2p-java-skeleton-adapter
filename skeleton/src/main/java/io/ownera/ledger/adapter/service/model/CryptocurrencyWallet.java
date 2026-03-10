package io.ownera.ledger.adapter.service.model;

public class CryptocurrencyWallet implements DestinationAccount {
    public final String address;

    public CryptocurrencyWallet(String address) {
        this.address = address;
    }
}
