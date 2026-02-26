package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import io.ownera.ledger.adapter.service.workflow.OperationRecord;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
import org.jooq.DSLContext;

import javax.annotation.Nullable;

import static io.ownera.ledger.adapter.db.generated.Tables.OPERATION_RECORDS;

public class JdbcOperationStore implements OperationStore {

    private final DSLContext dsl;

    public JdbcOperationStore(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    @Nullable
    public OperationRecord findByInputsHash(String inputsHash) {
        return dsl.selectFrom(OPERATION_RECORDS)
                .where(OPERATION_RECORDS.INPUTS_HASH.eq(inputsHash))
                .fetchOptional()
                .map(this::toOperationRecord)
                .orElse(null);
    }

    @Override
    public void save(OperationRecord record) {
        dsl.insertInto(OPERATION_RECORDS)
                .set(OPERATION_RECORDS.CID, record.cid)
                .set(OPERATION_RECORDS.METHOD, record.method)
                .set(OPERATION_RECORDS.STATUS, record.status.name())
                .set(OPERATION_RECORDS.INPUTS_HASH, record.inputsHash)
                .execute();
    }

    @Override
    public void updateStatus(String cid, OperationRecord.Status status, @Nullable OperationStatus result) {
        dsl.update(OPERATION_RECORDS)
                .set(OPERATION_RECORDS.STATUS, status.name())
                .where(OPERATION_RECORDS.CID.eq(cid))
                .execute();
    }

    @Override
    @Nullable
    public OperationRecord findByCid(String cid) {
        return dsl.selectFrom(OPERATION_RECORDS)
                .where(OPERATION_RECORDS.CID.eq(cid))
                .fetchOptional()
                .map(this::toOperationRecord)
                .orElse(null);
    }

    private OperationRecord toOperationRecord(org.jooq.Record r) {
        return new OperationRecord(
                r.get(OPERATION_RECORDS.CID),
                r.get(OPERATION_RECORDS.METHOD),
                OperationRecord.Status.valueOf(r.get(OPERATION_RECORDS.STATUS)),
                r.get(OPERATION_RECORDS.INPUTS_HASH),
                null
        );
    }
}
