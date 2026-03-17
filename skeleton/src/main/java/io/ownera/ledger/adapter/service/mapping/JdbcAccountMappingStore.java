package io.ownera.ledger.adapter.service.mapping;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Database-backed implementation of {@link AccountMappingStore}.
 * Uses the {@code ledger_adapter.account_mappings} table defined in V1001__create_ledger_adapter_schema.sql.
 * <p>
 * Uses jOOQ plain DSL (no codegen) so the skeleton stays self-contained.
 */
public class JdbcAccountMappingStore implements AccountMappingStore {

    private static final Table<?> TABLE = DSL.table(DSL.name("ledger_adapter", "account_mappings"));
    private static final Field<String> FIN_ID = DSL.field(DSL.name("fin_id"), String.class);
    private static final Field<String> ACCOUNT = DSL.field(DSL.name("account"), String.class);
    private static final Field<OffsetDateTime> CREATED_AT = DSL.field(DSL.name("created_at"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<OffsetDateTime> UPDATED_AT = DSL.field(DSL.name("updated_at"), SQLDataType.TIMESTAMPWITHTIMEZONE);

    private final DSLContext dsl;

    public JdbcAccountMappingStore(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<AccountMapping> getByFinId(String finId) {
        return dsl.selectFrom(TABLE)
                .where(FIN_ID.eq(finId))
                .orderBy(CREATED_AT.asc(), ACCOUNT.asc())
                .fetch()
                .stream()
                .map(this::toAccountMapping)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountMapping> getByAccount(String account) {
        return dsl.selectFrom(TABLE)
                .where(ACCOUNT.eq(account.toLowerCase()))
                .orderBy(CREATED_AT.asc(), FIN_ID.asc())
                .fetch()
                .stream()
                .map(this::toAccountMapping)
                .collect(Collectors.toList());
    }

    @Override
    public AccountMapping save(String finId, String account) {
        String normalized = account.toLowerCase();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        dsl.insertInto(TABLE)
                .set(FIN_ID, finId)
                .set(ACCOUNT, normalized)
                .set(CREATED_AT, now)
                .set(UPDATED_AT, now)
                .onConflict(FIN_ID, ACCOUNT)
                .doNothing()
                .execute();

        // Return existing or newly created record
        Record r = dsl.selectFrom(TABLE)
                .where(FIN_ID.eq(finId).and(ACCOUNT.eq(normalized)))
                .fetchOne();

        return r != null ? toAccountMapping(r) : new AccountMapping(finId, normalized,
                now.toInstant(), now.toInstant());
    }

    @Override
    public void delete(String finId, String account) {
        if (account != null) {
            dsl.deleteFrom(TABLE)
                    .where(FIN_ID.eq(finId).and(ACCOUNT.eq(account.toLowerCase())))
                    .execute();
        } else {
            dsl.deleteFrom(TABLE)
                    .where(FIN_ID.eq(finId))
                    .execute();
        }
    }

    @Override
    public List<AccountMapping> listAll() {
        return dsl.selectFrom(TABLE)
                .orderBy(CREATED_AT.asc())
                .fetch()
                .stream()
                .map(this::toAccountMapping)
                .collect(Collectors.toList());
    }

    private AccountMapping toAccountMapping(Record r) {
        OffsetDateTime createdAt = r.get(CREATED_AT);
        OffsetDateTime updatedAt = r.get(UPDATED_AT);
        return new AccountMapping(
                r.get(FIN_ID),
                r.get(ACCOUNT),
                createdAt != null ? createdAt.toInstant() : Instant.now(),
                updatedAt != null ? updatedAt.toInstant() : Instant.now()
        );
    }
}
