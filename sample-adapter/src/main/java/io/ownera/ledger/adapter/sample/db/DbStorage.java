package io.ownera.ledger.adapter.sample.db;

import io.ownera.ledger.adapter.sample.HoldOperation;
import io.ownera.ledger.adapter.service.TokenServiceException;
import io.ownera.ledger.adapter.service.model.Asset;
import org.jooq.DSLContext;

import static io.ownera.ledger.adapter.db.generated.default_schema.Tables.*;
import static io.ownera.ledger.adapter.db.generated.ledger_adapter.Tables.ASSETS;

public class DbStorage {

    private final DSLContext dsl;

    public DbStorage(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void createAsset(String assetId, Asset asset) {
        dsl.insertInto(ASSETS)
                .set(ASSETS.TYPE, asset.assetType.name())
                .set(ASSETS.ID, assetId)
                .set(ASSETS.TOKEN_STANDARD, "ERC20")
                .set(ASSETS.DECIMALS, 0)
                .set(ASSETS.CONTRACT_ADDRESS, "")
                .onConflictDoNothing()
                .execute();
    }

    public void checkAssetExists(String assetId) {
        int count = dsl.fetchCount(ASSETS, ASSETS.ID.eq(assetId));
        if (count == 0) {
            throw new TokenServiceException("Asset " + assetId + " not found");
        }
    }

    public void saveHoldOperation(String operationId, String finId, String quantity) {
        dsl.insertInto(HOLD_OPERATIONS)
                .set(HOLD_OPERATIONS.OPERATION_ID, operationId)
                .set(HOLD_OPERATIONS.FIN_ID, finId)
                .set(HOLD_OPERATIONS.QUANTITY, quantity)
                .execute();
    }

    public HoldOperation getHoldOperation(String operationId) {
        return dsl.selectFrom(HOLD_OPERATIONS)
                .where(HOLD_OPERATIONS.OPERATION_ID.eq(operationId))
                .fetchOptional()
                .map(r -> new HoldOperation(r.get(HOLD_OPERATIONS.FIN_ID), r.get(HOLD_OPERATIONS.QUANTITY)))
                .orElse(null);
    }

    public void removeHoldOperation(String operationId) {
        dsl.deleteFrom(HOLD_OPERATIONS)
                .where(HOLD_OPERATIONS.OPERATION_ID.eq(operationId))
                .execute();
    }

    public String getBalance(String finId, String assetId) {
        checkAssetExists(assetId);
        return dsl.select(BALANCES.BALANCE)
                .from(BALANCES)
                .where(BALANCES.FIN_ID.eq(finId).and(BALANCES.ASSET_ID.eq(assetId)))
                .fetchOptional(BALANCES.BALANCE)
                .map(Object::toString)
                .orElse("0");
    }

    public void debit(String from, String quantity, String assetId) {
        checkAssetExists(assetId);
        int amount = Integer.parseInt(quantity);
        int currentBalance = getCurrentBalance(from, assetId);
        if (currentBalance < amount) {
            throw new TokenServiceException("Insufficient balance for asset " + assetId);
        }
        dsl.update(BALANCES)
                .set(BALANCES.BALANCE, BALANCES.BALANCE.minus(amount))
                .where(BALANCES.FIN_ID.eq(from).and(BALANCES.ASSET_ID.eq(assetId)))
                .execute();
    }

    public void credit(String to, String quantity, String assetId) {
        checkAssetExists(assetId);
        int amount = Integer.parseInt(quantity);
        dsl.insertInto(BALANCES)
                .set(BALANCES.FIN_ID, to)
                .set(BALANCES.ASSET_ID, assetId)
                .set(BALANCES.BALANCE, amount)
                .onConflict(BALANCES.FIN_ID, BALANCES.ASSET_ID)
                .doUpdate()
                .set(BALANCES.BALANCE, BALANCES.BALANCE.plus(amount))
                .execute();
    }

    public void move(String from, String to, String quantity, String assetId) {
        checkAssetExists(assetId);
        debit(from, quantity, assetId);
        credit(to, quantity, assetId);
    }

    private int getCurrentBalance(String finId, String assetId) {
        return dsl.select(BALANCES.BALANCE)
                .from(BALANCES)
                .where(BALANCES.FIN_ID.eq(finId).and(BALANCES.ASSET_ID.eq(assetId)))
                .fetchOptional(BALANCES.BALANCE)
                .orElse(0);
    }
}
