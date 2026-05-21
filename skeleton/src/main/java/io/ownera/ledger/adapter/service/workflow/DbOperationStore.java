package io.ownera.ledger.adapter.service.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ownera.ledger.adapter.Mappers;
import io.ownera.ledger.adapter.api.model.APIOperationStatus;
import io.ownera.ledger.adapter.service.model.OperationStatus;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Database-backed implementation of {@link OperationStore}.
 * Uses the {@code <schema>.operations} table. Schema defaults to {@code ledger_adapter}.
 * Uses jOOQ plain DSL (no codegen) so the skeleton stays self-contained.
 */
public class DbOperationStore implements OperationStore {

    private static final Logger logger = LoggerFactory.getLogger(DbOperationStore.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Field<String> CID = DSL.field(DSL.name("cid"), String.class);
    private static final Field<String> METHOD = DSL.field(DSL.name("method"), String.class);
    private static final Field<String> STATUS = DSL.field(DSL.name("status"), String.class);
    private static final Field<String> INPUTS_HASH = DSL.field(DSL.name("inputs_hash"), String.class);
    private static final Field<JSONB> INPUTS  = DSL.field(DSL.name("inputs"),  SQLDataType.JSONB);
    private static final Field<JSONB> OUTPUTS = DSL.field(DSL.name("outputs"), SQLDataType.JSONB);

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
    public void save(OperationRecord record, @Nullable String pendingOutputsJson) {
        var insert = dsl.insertInto(table)
                .set(CID, record.cid)
                .set(METHOD, record.method)
                .set(STATUS, record.status.name())
                .set(INPUTS_HASH, record.inputsHash);
        if (pendingOutputsJson != null) {
            insert = insert.set(OUTPUTS, JSONB.valueOf(pendingOutputsJson));
        }
        insert.execute();
    }

    @Override
    public void updateStatus(String cid, OperationRecord.Status status, @Nullable String outputsJson) {
        if (outputsJson != null) {
            dsl.update(table)
                    .set(STATUS, status.name())
                    .set(OUTPUTS, JSONB.valueOf(outputsJson))
                    .where(CID.eq(cid))
                    .execute();
        } else {
            dsl.update(table)
                    .set(STATUS, status.name())
                    .where(CID.eq(cid))
                    .execute();
        }
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

    @Override
    @Nullable
    public String findOutputsByCid(String cid) {
        JSONB row = dsl.select(OUTPUTS)
                .from(table)
                .where(CID.eq(cid))
                .fetchOne(OUTPUTS);
        return row != null ? row.data() : null;
    }

    private OperationRecord toRecord(org.jooq.Record r) {
        // Parse the persisted outputs JSON back into an internal OperationStatus so the
        // idempotent-replay cache-hit path in OperationExecutor.execute can return the
        // original completed payload (instead of falling through to Pending). Mirrors
        // Node's createServiceProxy → "if (!inserted) return storageOperation.outputs"
        // (skeleton/src/workflows/service.ts). Best-effort: a parse failure is logged but
        // does not break record lookup — execute() simply falls back to Pending.
        OperationStatus result = null;
        JSONB outputs = r.get(OUTPUTS);
        if (outputs != null && outputs.data() != null && !outputs.data().isEmpty()) {
            try {
                APIOperationStatus api = MAPPER.readValue(outputs.data(), APIOperationStatus.class);
                result = Mappers.fromAPI(api);
            } catch (Exception e) {
                logger.warn("Failed to parse persisted outputs for cid={}: {}", r.get(CID), e.getMessage());
            }
        }
        return new OperationRecord(
                r.get(CID),
                r.get(METHOD),
                OperationRecord.Status.valueOf(r.get(STATUS)),
                r.get(INPUTS_HASH),
                result
        );
    }
}
