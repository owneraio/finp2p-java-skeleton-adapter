package io.ownera.ledger.adapter.sample.db;

import io.ownera.ledger.adapter.sample.HoldOperation;
import io.ownera.ledger.adapter.service.TokenServiceException;
import io.ownera.ledger.adapter.service.model.Asset;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static io.ownera.ledger.adapter.db.generated.Tables.*;

public class DbStorage {

    // Skeleton-owned table — uses plain DSL since schema name is configurable
    private static final Field<String> ASSET_TYPE = DSL.field(DSL.name("type"), String.class);
    private static final Field<String> ASSET_ID = DSL.field(DSL.name("id"), String.class);
    private static final Field<String> TOKEN_STANDARD = DSL.field(DSL.name("token_standard"), String.class);
    private static final Field<Integer> DECIMALS = DSL.field(DSL.name("decimals"), Integer.class);

    private final DSLContext dsl;
    private final Table<?> assetsTable;

    public DbStorage(DSLContext dsl) {
        this(dsl, "ledger_adapter");
    }

    public DbStorage(DSLContext dsl, String schemaName) {
        this.dsl = dsl;
        this.assetsTable = DSL.table(DSL.name(schemaName, "assets"));
    }

    public void createAsset(String assetId, Asset asset) {
        dsl.insertInto(assetsTable)
                .set(ASSET_TYPE, asset.assetType.name())
                .set(ASSET_ID, assetId)
                .set(TOKEN_STANDARD, "ERC20")
                .set(DECIMALS, 0)
                .onConflictDoNothing()
                .execute();
    }

    public void checkAssetExists(Asset asset) {
        int count = dsl.fetchCount(assetsTable, ASSET_TYPE.eq(asset.assetType.name()).and(ASSET_ID.eq(asset.assetId)));
        if (count == 0) {
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
