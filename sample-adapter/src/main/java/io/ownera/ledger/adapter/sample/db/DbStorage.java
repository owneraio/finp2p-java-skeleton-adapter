package io.ownera.ledger.adapter.sample.db;

import io.ownera.ledger.adapter.sample.HoldOperation;
import io.ownera.ledger.adapter.service.TokenServiceException;
import io.ownera.ledger.adapter.service.asset.AssetStore;
import io.ownera.ledger.adapter.service.asset.DbAssetStore;
import io.ownera.ledger.adapter.service.model.Asset;
import org.jooq.DSLContext;

import static io.ownera.ledger.adapter.db.generated.Tables.*;

public class DbStorage {

    private final DSLContext dsl;
    private final AssetStore assetStore;

    public DbStorage(DSLContext dsl) {
        this(dsl, "ledger_adapter");
    }

    public DbStorage(DSLContext dsl, String schemaName) {
        this(dsl, new DbAssetStore(dsl, schemaName));
    }

    public DbStorage(DSLContext dsl, AssetStore assetStore) {
        this.dsl = dsl;
        this.assetStore = assetStore;
    }

    public void createAsset(String assetId, Asset asset) {
        // Persist via skeleton-owned AssetStore so all adapters share the same contract;
        // assetId always wins (the wrapping Asset.assetId may differ for legacy callers).
        Asset toSave = new Asset(assetId, asset.assetType, asset.ledgerIdentifier);
        assetStore.save(toSave);
    }

    public void checkAssetExists(Asset asset) {
        if (!assetStore.exists(asset.assetId)) {
            throw new TokenServiceException("Asset " + asset.assetId + " not found");
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

    public String getBalance(String finId, Asset asset) {
        checkAssetExists(asset);
        return dsl.select(BALANCES.BALANCE)
                .from(BALANCES)
                .where(BALANCES.FIN_ID.eq(finId).and(BALANCES.ASSET_ID.eq(asset.assetId)))
                .fetchOptional(BALANCES.BALANCE)
                .map(Object::toString)
                .orElse("0");
    }

    public void debit(String from, String quantity, Asset asset) {
        checkAssetExists(asset);
        int amount = Integer.parseInt(quantity);
        int currentBalance = getCurrentBalance(from, asset.assetId);
        if (currentBalance < amount) {
            throw new TokenServiceException("Insufficient balance for asset " + asset.assetId);
        }
        dsl.update(BALANCES)
                .set(BALANCES.BALANCE, BALANCES.BALANCE.minus(amount))
                .where(BALANCES.FIN_ID.eq(from).and(BALANCES.ASSET_ID.eq(asset.assetId)))
                .execute();
    }

    public void credit(String to, String quantity, Asset asset) {
        checkAssetExists(asset);
        int amount = Integer.parseInt(quantity);
        dsl.insertInto(BALANCES)
                .set(BALANCES.FIN_ID, to)
                .set(BALANCES.ASSET_ID, asset.assetId)
                .set(BALANCES.BALANCE, amount)
                .onConflict(BALANCES.FIN_ID, BALANCES.ASSET_ID)
                .doUpdate()
                .set(BALANCES.BALANCE, BALANCES.BALANCE.plus(amount))
                .execute();
    }

    public void move(String from, String to, String quantity, Asset asset) {
        checkAssetExists(asset);
        debit(from, quantity, asset);
        credit(to, quantity, asset);
    }

    private int getCurrentBalance(String finId, String assetId) {
        return dsl.select(BALANCES.BALANCE)
                .from(BALANCES)
                .where(BALANCES.FIN_ID.eq(finId).and(BALANCES.ASSET_ID.eq(assetId)))
                .fetchOptional(BALANCES.BALANCE)
                .orElse(0);
    }
}
