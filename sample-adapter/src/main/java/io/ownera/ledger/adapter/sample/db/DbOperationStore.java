package io.ownera.ledger.adapter.sample.db;

import io.ownera.ledger.adapter.service.workflow.OperationRecord;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DB-backed operation store using jOOQ plain DSL (no codegen).
 * Aligned with Node.js skeleton's operations table layout.
 */
public class DbOperationStore implements OperationStore {

    private static final Table<?> TABLE = DSL.table(DSL.name("ledger_adapter", "operations"));
    private static final Field<String> CID = DSL.field(DSL.name("cid"), String.class);
    private static final Field<String> METHOD = DSL.field(DSL.name("method"), String.class);
    private static final Field<String> STATUS = DSL.field(DSL.name("status"), String.class);
    private static final Field<JSONB> INPUTS = DSL.field(DSL.name("inputs"), JSONB.class);
    private static final Field<JSONB> OUTPUTS = DSL.field(DSL.name("outputs"), JSONB.class);

    private final DSLContext dsl;

    public DbOperationStore(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    @Nullable
    public OperationRecord findByInputs(String inputsJson) {
        return dsl.selectFrom(TABLE)
                .where(INPUTS.eq(JSONB.jsonb(inputsJson)))
                .fetchOptional()
                .map(this::toRecord)
                .orElse(null);
    }

    @Override
    public OperationRecord save(OperationRecord record) {
        // Use ON CONFLICT for idempotency — if inputs already exist, return existing
        int inserted = dsl.insertInto(TABLE)
                .set(CID, record.cid)
                .set(METHOD, record.method)
                .set(STATUS, record.status.dbValue())
                .set(INPUTS, JSONB.jsonb(record.inputs))
                .set(OUTPUTS, JSONB.jsonb(record.outputs != null ? record.outputs : "{}"))
                .onConflict(INPUTS)
                .doNothing()
                .execute();

        if (inserted == 0) {
            // Conflict — return existing record
            OperationRecord existing = findByInputs(record.inputs);
            return existing != null ? existing : record;
        }
        return record;
    }

    @Override
    public void updateStatus(String cid, OperationRecord.Status status, @Nullable String outputsJson) {
        var update = dsl.update(TABLE)
                .set(STATUS, status.dbValue());
        if (outputsJson != null) {
            update = update.set(OUTPUTS, JSONB.jsonb(outputsJson));
        }
        update.where(CID.eq(cid)).execute();
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

    @Override
    public List<OperationRecord> findPendingByMethod(String method) {
        return dsl.selectFrom(TABLE)
                .where(METHOD.eq(method).and(STATUS.eq(OperationRecord.Status.IN_PROGRESS.dbValue())))
                .fetch()
                .stream()
                .map(this::toRecord)
                .collect(Collectors.toList());
    }

    private OperationRecord toRecord(org.jooq.Record r) {
        JSONB inputsJsonb = r.get(INPUTS);
        JSONB outputsJsonb = r.get(OUTPUTS);
        return new OperationRecord(
                r.get(CID),
                r.get(METHOD),
                OperationRecord.Status.fromDbValue(r.get(STATUS)),
                inputsJsonb != null ? inputsJsonb.data() : "{}",
                outputsJsonb != null ? outputsJsonb.data() : null
        );
    }
}
