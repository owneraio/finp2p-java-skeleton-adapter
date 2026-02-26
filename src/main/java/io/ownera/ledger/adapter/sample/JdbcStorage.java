package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.TokenServiceException;
import io.ownera.ledger.adapter.service.model.Asset;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class JdbcStorage {

    private final JdbcTemplate jdbc;

    public JdbcStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void createAsset(String assetId, Asset asset) {
        jdbc.update(
                "INSERT INTO assets (asset_id, asset_type) VALUES (?, ?) ON CONFLICT DO NOTHING",
                assetId, asset.assetType.name());
    }

    public void checkAssetExists(String assetId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM assets WHERE asset_id = ?", Integer.class, assetId);
        if (count == null || count == 0) {
            throw new TokenServiceException("Asset " + assetId + " not found");
        }
    }

    public void saveHoldOperation(String operationId, String finId, String quantity) {
        jdbc.update(
                "INSERT INTO hold_operations (operation_id, fin_id, quantity) VALUES (?, ?, ?)",
                operationId, finId, quantity);
    }

    public HoldOperation getHoldOperation(String operationId) {
        List<HoldOperation> results = jdbc.query(
                "SELECT fin_id, quantity FROM hold_operations WHERE operation_id = ?",
                (rs, rowNum) -> new HoldOperation(rs.getString("fin_id"), rs.getString("quantity")),
                operationId);
        return results.isEmpty() ? null : results.get(0);
    }

    public void removeHoldOperation(String operationId) {
        jdbc.update("DELETE FROM hold_operations WHERE operation_id = ?", operationId);
    }

    public String getBalance(String finId, String assetId) {
        checkAssetExists(assetId);
        List<Integer> results = jdbc.query(
                "SELECT balance FROM balances WHERE fin_id = ? AND asset_id = ?",
                (rs, rowNum) -> rs.getInt("balance"),
                finId, assetId);
        if (results.isEmpty()) {
            return "0";
        }
        return Integer.toString(results.get(0));
    }

    public void debit(String from, String quantity, String assetId) {
        checkAssetExists(assetId);
        int amount = Integer.parseInt(quantity);
        int currentBalance = getCurrentBalance(from, assetId);
        if (currentBalance < amount) {
            throw new TokenServiceException("Insufficient balance for asset " + assetId);
        }
        jdbc.update(
                "UPDATE balances SET balance = balance - ? WHERE fin_id = ? AND asset_id = ?",
                amount, from, assetId);
    }

    public void credit(String to, String quantity, String assetId) {
        checkAssetExists(assetId);
        int amount = Integer.parseInt(quantity);
        jdbc.update(
                "INSERT INTO balances (fin_id, asset_id, balance) VALUES (?, ?, ?) " +
                        "ON CONFLICT (fin_id, asset_id) DO UPDATE SET balance = balances.balance + EXCLUDED.balance",
                to, assetId, amount);
    }

    public void move(String from, String to, String quantity, String assetId) {
        checkAssetExists(assetId);
        debit(from, quantity, assetId);
        credit(to, quantity, assetId);
    }

    private int getCurrentBalance(String finId, String assetId) {
        List<Integer> results = jdbc.query(
                "SELECT balance FROM balances WHERE fin_id = ? AND asset_id = ?",
                (rs, rowNum) -> rs.getInt("balance"),
                finId, assetId);
        return results.isEmpty() ? 0 : results.get(0);
    }
}
