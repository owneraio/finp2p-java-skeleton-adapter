package io.ownera.ledger.adapter.service.asset;

import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.AssetType;
import io.ownera.ledger.adapter.service.model.LedgerAssetIdentifier;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

/**
 * Database-backed {@link AssetStore}.
 * Uses the {@code <schema>.assets} table. Schema defaults to {@code ledger_adapter}.
 * Uses jOOQ plain DSL (no codegen) so the skeleton stays self-contained.
 */
public class DbAssetStore implements AssetStore {

    private static final Field<String> TYPE = DSL.field(DSL.name("type"), String.class);
    private static final Field<String> ID = DSL.field(DSL.name("id"), String.class);
    private static final Field<String> TOKEN_STANDARD = DSL.field(DSL.name("token_standard"), String.class);
    private static final Field<Integer> DECIMALS = DSL.field(DSL.name("decimals"), Integer.class);
    private static final Field<String> TOKEN_ID = DSL.field(DSL.name("token_id"), String.class);
    private static final Field<String> NETWORK = DSL.field(DSL.name("network"), String.class);

    private final DSLContext dsl;
    private final Table<?> table;

    public DbAssetStore(DSLContext dsl) {
        this(dsl, "ledger_adapter");
    }

    public DbAssetStore(DSLContext dsl, String schemaName) {
        this.dsl = dsl;
        this.table = DSL.table(DSL.name(schemaName, "assets"));
    }

    @Override
    public void save(Asset asset) {
        LedgerAssetIdentifier id = asset.ledgerIdentifier;
        String tokenStandard = id != null && id.standard != null ? id.standard : "";
        String tokenId = id != null && id.tokenId != null ? id.tokenId : "";
        String network = id != null && id.network != null ? id.network : "";
        dsl.insertInto(table)
                .set(TYPE, asset.assetType.name())
                .set(ID, asset.assetId)
                .set(TOKEN_STANDARD, tokenStandard)
                .set(DECIMALS, 0)
                .set(TOKEN_ID, tokenId)
                .set(NETWORK, network)
                .onConflictDoNothing()
                .execute();
    }

    @Override
    public Asset getById(String assetId) {
        Record row = dsl.select(TYPE, ID, TOKEN_STANDARD, TOKEN_ID, NETWORK)
                .from(table)
                .where(ID.eq(assetId))
                .fetchOne();
        if (row == null) {
            return null;
        }
        AssetType type = AssetType.valueOf(row.get(TYPE));
        String tokenId = row.get(TOKEN_ID);
        String network = row.get(NETWORK);
        String standard = row.get(TOKEN_STANDARD);
        LedgerAssetIdentifier ledgerId = (tokenId != null && !tokenId.isEmpty())
                ? new LedgerAssetIdentifier(network, tokenId, standard)
                : null;
        return new Asset(row.get(ID), type, ledgerId);
    }

    @Override
    public boolean exists(String assetId) {
        return dsl.fetchCount(table, ID.eq(assetId)) > 0;
    }
}
