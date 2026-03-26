package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

import javax.annotation.Nullable;

/**
 * Database-backed implementation of {@link OperationStore}.
 * Uses the {@code <schema>.operations} table. Schema defaults to {@code ledger_adapter}.
 * Uses jOOQ plain DSL (no codegen) so the skeleton stays self-contained.
 */
public class DbOperationStore implements OperationStore {

    private static final Field<String> CID = DSL.field(DSL.name("cid"), String.class);
    private static final Field<String> METHOD = DSL.field(DSL.name("method"), String.class);
    private static final Field<String> STATUS = DSL.field(DSL.name("status"), String.class);
    private static final Field<String> INPUTS_HASH = DSL.field(DSL.name("inputs_hash"), String.class);

    private final DSLContext dsl;
    private final Table<?> table;

    public DbOperationStore(DSLContext dsl) {
        this(dsl, "ledger_adapter");
    }

    public DbOperationStore(DSLContext dsl, String schemaName) {
        this.dsl = dsl;
        this.table = DSL.table(DSL.name(schemaName, "operations"));
    }

    @Override
    @Nullable
    public OperationRecord findByInputsHash(String inputsHash) {
        return dsl.selectFrom(table)
                .where(INPUTS_HASH.eq(inputsHash))
                .fetchOptional()
                .map(this::toRecord)
                .orElse(null);
    }

    @Override
    public void save(OperationRecord record) {
        dsl.insertInto(table)
                .set(CID, record.cid)
                .set(METHOD, record.method)
                .set(STATUS, record.status.name())
                .set(INPUTS_HASH, record.inputsHash)
                .execute();
    }

    @Override
    public void updateStatus(String cid, OperationRecord.Status status, @Nullable OperationStatus result) {
        dsl.update(table)
                .set(STATUS, status.name())
                .where(CID.eq(cid))
                .execute();
    }

    @Override
    @Nullable
    public OperationRecord findByCid(String cid) {
        return dsl.selectFrom(table)
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
