package io.ownera.ledger.adapter.sample.db;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import io.ownera.ledger.adapter.service.workflow.OperationRecord;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
import org.jooq.DSLContext;

import javax.annotation.Nullable;

import static io.ownera.ledger.adapter.db.generated.ledger_adapter.Tables.OPERATIONS;

public class DbOperationStore implements OperationStore {

    private final DSLContext dsl;

    public DbOperationStore(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    @Nullable
    public OperationRecord findByInputsHash(String inputsHash) {
        return dsl.selectFrom(OPERATIONS)
                .where(OPERATIONS.INPUTS_HASH.eq(inputsHash))
                .fetchOptional()
                .map(this::toOperationRecord)
                .orElse(null);
    }

    @Override
    public void save(OperationRecord record) {
        dsl.insertInto(OPERATIONS)
                .set(OPERATIONS.CID, record.cid)
                .set(OPERATIONS.METHOD, record.method)
                .set(OPERATIONS.STATUS, record.status.name())
                .set(OPERATIONS.INPUTS_HASH, record.inputsHash)
                .execute();
    }

    @Override
    public void updateStatus(String cid, OperationRecord.Status status, @Nullable OperationStatus result) {
        dsl.update(OPERATIONS)
                .set(OPERATIONS.STATUS, status.name())
                .where(OPERATIONS.CID.eq(cid))
                .execute();
    }

    @Override
    @Nullable
    public OperationRecord findByCid(String cid) {
        return dsl.selectFrom(OPERATIONS)
                .where(OPERATIONS.CID.eq(cid))
                .fetchOptional()
                .map(this::toOperationRecord)
                .orElse(null);
    }

    private OperationRecord toOperationRecord(org.jooq.Record r) {
        return new OperationRecord(
                r.get(OPERATIONS.CID),
                r.get(OPERATIONS.METHOD),
                OperationRecord.Status.valueOf(r.get(OPERATIONS.STATUS)),
                r.get(OPERATIONS.INPUTS_HASH),
                null
        );
    }
}
