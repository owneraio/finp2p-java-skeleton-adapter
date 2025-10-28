package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.TokenServiceException;

import java.util.HashMap;
import java.util.Map;

public class Account {

    private final Map<String, Integer> balances = new HashMap<>();

    public int balance(String assetCode) {
        return this.balances.getOrDefault(assetCode, 0);
    }

    public void debit(String assetCode, String quantity) {
        int amount = Integer.parseInt(quantity);
        int currentBalance = this.balances.getOrDefault(assetCode, 0);
        if (currentBalance < amount) {
            throw new TokenServiceException("Insufficient balance for asset " + assetCode);
        }
        this.balances.put(assetCode, currentBalance - amount);
    }

    public void credit(String assetCode, String quantity) {
        int amount = Integer.parseInt(quantity);
        int currentBalance = this.balances.getOrDefault(assetCode, 0);
        this.balances.put(assetCode, currentBalance + amount);
    }

}
