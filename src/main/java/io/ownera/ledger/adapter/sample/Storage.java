package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.model.Asset;

import java.util.HashMap;
import java.util.Map;

public class Storage {

    private final Map<String, Asset> assets = new HashMap<>();
    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, HoldOperation> holdOperations = new HashMap<>();

    public void createAsset(String assetId, Asset asset) {
        this.assets.put(assetId, asset);
    }

    public void checkAssetExists(String assetId) {
        Asset asset = this.assets.get(assetId);
        if (asset == null) {
            throw new RuntimeException("Asset " + assetId + " not found");
        }
    }

    public void saveHoldOperation(String operationId, String finId, String quantity) {
        this.holdOperations.put(operationId, new HoldOperation(finId, quantity));
    }

    public HoldOperation getHoldOperation(String operationId) {
        return this.holdOperations.get(operationId);
    }

    public void removeHoldOperation(String operationId) {
        this.holdOperations.remove(operationId);
    }

    public String getBalance(String finId, String assetId) {
        this.checkAssetExists(assetId);
        Account account = this.accounts.get(finId);
        if (account == null) {
            return "0";
        }
        int balance = account.balance(assetId);
        return Integer.toString(balance);
    }

    public void debit(String from, String quantity, String assetId) {
        this.checkAssetExists(assetId);
        this.getAccount(from).debit(assetId, quantity);
    }

    public void credit(String to, String quantity, String assetId) {
        this.checkAssetExists(assetId);
        this.getOrCreateAccount(to).credit(assetId, quantity);
    }

    public void move(String from, String to, String quantity, String assetId) {
        this.checkAssetExists(assetId);
        this.getAccount(from).debit(assetId, quantity);
        this.getOrCreateAccount(to).credit(assetId, quantity);
    }

    public Account getAccount(String finId) {
        Account account = this.accounts.get(finId);
        if (account == null) {
            throw new RuntimeException("Account " + finId + " not found");
        }
        return account;
    }

    public Account getOrCreateAccount(String finId) {
        Account account = this.accounts.get(finId);
        if (account != null) {
            return account;
        } else {
            Account newAccount = new Account();
            this.accounts.put(finId, newAccount);
            return newAccount;
        }
    }

}
