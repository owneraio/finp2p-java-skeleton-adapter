package io.ownera.ledger.adapter.service.mapping;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Database-backed implementation of {@link AccountMappingStore}.
 * Uses the {@code ledger_adapter.account_mappings} key-value table.
 * Uses jOOQ plain DSL (no codegen) so the skeleton stays self-contained.
 */
public class JdbcAccountMappingStore implements AccountMappingStore {

    private static final Table<?> TABLE = DSL.table(DSL.name("ledger_adapter", "account_mappings"));
    private static final Field<String> FIN_ID = DSL.field(DSL.name("fin_id"), String.class);
    private static final Field<String> FIELD_NAME = DSL.field(DSL.name("field_name"), String.class);
    private static final Field<String> VALUE = DSL.field(DSL.name("value"), String.class);
    private static final Field<OffsetDateTime> CREATED_AT = DSL.field(DSL.name("created_at"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<OffsetDateTime> UPDATED_AT = DSL.field(DSL.name("updated_at"), SQLDataType.TIMESTAMPWITHTIMEZONE);

    private final DSLContext dsl;

    public JdbcAccountMappingStore(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public AccountMapping getByFinId(String finId) {
        Result<?> rows = dsl.selectFrom(TABLE)
                .where(FIN_ID.eq(finId))
                .orderBy(FIELD_NAME.asc())
                .fetch();

        if (rows.isEmpty()) return null;
        return toAccountMapping(finId, new ArrayList<>(rows));
    }

    @Override
    public List<AccountMapping> getByFieldValue(String fieldName, String value) {
        List<String> finIds = dsl.selectDistinct(FIN_ID)
                .from(TABLE)
                .where(FIELD_NAME.eq(fieldName).and(VALUE.eq(value.toLowerCase())))
                .fetch(FIN_ID);

        return finIds.stream()
                .map(this::getByFinId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void save(String finId, Map<String, String> fields) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String normalized = entry.getValue().toLowerCase();
            dsl.insertInto(TABLE)
                    .set(FIN_ID, finId)
                    .set(FIELD_NAME, entry.getKey())
                    .set(VALUE, normalized)
                    .set(CREATED_AT, now)
                    .set(UPDATED_AT, now)
                    .onConflict(FIN_ID, FIELD_NAME)
                    .doUpdate()
                    .set(VALUE, normalized)
                    .set(UPDATED_AT, now)
                    .execute();
        }
    }

    @Override
    public void delete(String finId, String fieldName) {
        if (fieldName != null) {
            dsl.deleteFrom(TABLE)
                    .where(FIN_ID.eq(finId).and(FIELD_NAME.eq(fieldName)))
                    .execute();
        } else {
            dsl.deleteFrom(TABLE)
                    .where(FIN_ID.eq(finId))
                    .execute();
        }
    }

    @Override
    public List<AccountMapping> listAll() {
        Result<?> allRows = dsl.selectFrom(TABLE)
                .orderBy(FIN_ID.asc(), FIELD_NAME.asc())
                .fetch();

        // Group by finId
        Map<String, List<Record>> grouped = new ArrayList<Record>(allRows).stream()
                .collect(Collectors.groupingBy(r -> r.get(FIN_ID), LinkedHashMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .map(e -> toAccountMapping(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private AccountMapping toAccountMapping(String finId, List<Record> rows) {
        Map<String, String> fields = new LinkedHashMap<>();
        Instant earliest = Instant.MAX;
        Instant latest = Instant.MIN;

        for (Record r : rows) {
            fields.put(r.get(FIELD_NAME), r.get(VALUE));
            OffsetDateTime ca = r.get(CREATED_AT);
            OffsetDateTime ua = r.get(UPDATED_AT);
            if (ca != null && ca.toInstant().isBefore(earliest)) earliest = ca.toInstant();
            if (ua != null && ua.toInstant().isAfter(latest)) latest = ua.toInstant();
        }

        if (earliest == Instant.MAX) earliest = Instant.now();
        if (latest == Instant.MIN) latest = Instant.now();

        return new AccountMapping(finId, fields, earliest, latest);
    }
}
