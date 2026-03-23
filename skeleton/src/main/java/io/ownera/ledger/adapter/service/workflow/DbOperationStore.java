package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

import javax.annotation.Nullable;

/**
 * Database-backed implementation of {@link OperationStore}.
 * Uses the {@code ledger_adapter.operations} table.
 * Uses jOOQ plain DSL (no codegen) so the skeleton stays self-contained.
 */
public class DbOperationStore implements OperationStore {

    private static final Table<?> TABLE = DSL.table(DSL.name("ledger_adapter", "operations"));
    private static final Field<String> CID = DSL.field(DSL.name("cid"), String.class);
    private static final Field<String> METHOD = DSL.field(DSL.name("method"), String.class);
    private static final Field<String> STATUS = DSL.field(DSL.name("status"), String.class);
    private static final Field<String> INPUTS_HASH = DSL.field(DSL.name("inputs_hash"), String.class);

    private final DSLContext dsl;

    public DbOperationStore(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    @Nullable
    public OperationRecord findByInputsHash(String inputsHash) {
        return dsl.selectFrom(TABLE)
                .where(INPUTS_HASH.eq(inputsHash))
                .fetchOptional()
                .map(this::toRecord)
                .orElse(null);
    }

    @Override
    public void save(OperationRecord record) {
        dsl.insertInto(TABLE)
                .set(CID, record.cid)
                .set(METHOD, record.method)
                .set(STATUS, record.status.name())
                .set(INPUTS_HASH, record.inputsHash)
                .execute();
    }

    @Override
    public void updateStatus(String cid, OperationRecord.Status status, @Nullable OperationStatus result) {
        dsl.update(TABLE)
                .set(STATUS, status.name())
                .where(CID.eq(cid))
                .execute();
    }

    @Override
    @Nullable
    public OperationRecord findByCid(String cid) {
        return dsl.selectFrom(TABLE)
                .where(CID.eq(cid))
                .fetchOptional()
                .map(this::toRecord)
                .orElse(null);
    }

    private OperationRecord toRecord(org.jooq.Record r) {
        return new OperationRecord(
                r.get(CID),
                r.get(METHOD),
                OperationRecord.Status.valueOf(r.get(STATUS)),
                r.get(INPUTS_HASH),
                null
        );
    }
}
