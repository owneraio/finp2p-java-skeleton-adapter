package io.ownera.ledger.adapter.vanilla;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ownera.ledger.adapter.service.BusinessException;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Result;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * PostgreSQL-backed ledger storage for the vanilla service. Provides atomic balance operations
 * with idempotency via CTE-based queries, ported from the Node vanilla adapter
 * ({@code vanilla-service/src/storage.ts}).
 *
 * <p>The schema name qualifying {@code accounts} and {@code transactions} is configurable so
 * multiple adapters can share a database without colliding. It is validated against a strict
 * identifier regex to keep it safe to splice into raw SQL.
 *
 * <p>Concurrency: each mutating call takes a per-asset {@code pg_advisory_xact_lock} keyed on
 * {@code (asset_id, asset_type)}. Two operations on the same asset serialize; operations on
 * different assets run in parallel.
 */
public class LedgerStorage {

    private static final String DEFAULT_ASSET_TYPE = "finp2p";
    private static final Pattern SCHEMA_NAME_RE = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SecureRandom RNG = new SecureRandom();

    private final DSLContext dsl;
    private final String schema;
    private final String ensureAccountSql;
    private final String getBalanceSql;
    private final String getTransactionSql;
    private final String findByOperationIdSql;
    private final String transferSql;
    private final String listDistributedAccountsSql;
    private final String getDistributionStatusSql;
    private final String syncOmnibusBalanceSql;

    public LedgerStorage(DSLContext dsl, String schemaName) {
        if (!SCHEMA_NAME_RE.matcher(schemaName).matches()) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
        this.dsl = dsl;
        this.schema = schemaName;
        String accountsTable = schemaName + ".accounts";
        String transactionsTable = schemaName + ".transactions";
        this.ensureAccountSql           = String.format(ENSURE_ACCOUNT_TEMPLATE,            accountsTable);
        this.getBalanceSql              = String.format(GET_BALANCE_TEMPLATE,               accountsTable);
        this.getTransactionSql          = String.format(GET_TRANSACTION_TEMPLATE,           transactionsTable);
        this.findByOperationIdSql       = String.format(FIND_BY_OPERATION_ID_TEMPLATE,      transactionsTable);
        this.transferSql                = String.format(TRANSFER_SQL_TEMPLATE,              transactionsTable, accountsTable);
        this.listDistributedAccountsSql = String.format(LIST_DISTRIBUTED_ACCOUNTS_TEMPLATE, accountsTable);
        this.getDistributionStatusSql   = String.format(GET_DISTRIBUTION_STATUS_TEMPLATE,   accountsTable);
        this.syncOmnibusBalanceSql      = String.format(SYNC_OMNIBUS_BALANCE_TEMPLATE,      accountsTable);
    }

    // SQL templates. Table names are spliced once at construction via String.format — they're
    // validated against {@link #SCHEMA_NAME_RE} before any substitution, so splicing is safe.
    // All runtime values flow through {@code ?} bind parameters.

    private static final String ENSURE_ACCOUNT_TEMPLATE =
            "INSERT INTO %s (fin_id, asset_id, asset_type) " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT (fin_id, asset_id, asset_type) DO NOTHING";

    private static final String GET_BALANCE_TEMPLATE =
            "SELECT balance::TEXT AS balance, held::TEXT AS held, " +
                    "       (balance - held)::TEXT AS available " +
                    "FROM %s " +
                    "WHERE fin_id = ? AND asset_id = ? AND asset_type = ?";

    private static final String GET_TRANSACTION_TEMPLATE =
            "SELECT id, asset_id, asset_type, source, destination, " +
                    "       amount::TEXT AS amount, source_held::TEXT AS source_held, " +
                    "       destination_held::TEXT AS destination_held, " +
                    "       action, details, created_at " +
                    "FROM %s WHERE id = ?";

    private static final String FIND_BY_OPERATION_ID_TEMPLATE =
            "SELECT id, asset_id, asset_type, source, destination, " +
                    "       amount::TEXT AS amount, source_held::TEXT AS source_held, " +
                    "       destination_held::TEXT AS destination_held, " +
                    "       action, details, created_at " +
                    "FROM %s " +
                    "WHERE details->>'operation_id' = ? " +
                    "ORDER BY created_at DESC, id DESC " +
                    "LIMIT 1";

    private static final String LIST_DISTRIBUTED_ACCOUNTS_TEMPLATE =
            "SELECT fin_id, balance::TEXT AS balance " +
                    "FROM %s " +
                    "WHERE asset_id = ? AND asset_type = ? AND fin_id != ? AND balance > 0 " +
                    "ORDER BY fin_id";

    private static final String GET_DISTRIBUTION_STATUS_TEMPLATE =
            "SELECT COALESCE(o.balance, 0)::TEXT                          AS available, " +
                    "       COALESCE(d.total, 0)::TEXT                          AS distributed, " +
                    "       (COALESCE(o.balance, 0) + COALESCE(d.total, 0))::TEXT AS omnibus_balance " +
                    "FROM " +
                    "  (SELECT balance FROM %1$s " +
                    "   WHERE fin_id = ? AND asset_id = ? AND asset_type = ?) o " +
                    "FULL JOIN " +
                    "  (SELECT SUM(balance) AS total FROM %1$s " +
                    "   WHERE asset_id = ? AND asset_type = ? AND fin_id != ?) d ON TRUE";

    private static final String SYNC_OMNIBUS_BALANCE_TEMPLATE =
            "WITH lock_asset AS (" +
                    "  SELECT pg_advisory_xact_lock(hashtext(?), hashtext(?))" +
                    "), " +
                    "distributed AS (" +
                    "  SELECT COALESCE(SUM(a.balance), 0) AS total " +
                    "  FROM %1$s a, lock_asset " +
                    "  WHERE a.asset_id = ? AND a.asset_type = ? AND a.fin_id != ?" +
                    ") " +
                    "UPDATE %1$s a " +
                    "SET balance = CAST(? AS NUMERIC) - d.total, updated_at = NOW() " +
                    "FROM distributed d " +
                    "WHERE a.fin_id = ? AND a.asset_id = ? AND a.asset_type = ? " +
                    "RETURNING d.total::TEXT AS distributed, a.balance::TEXT AS available";

    /**
     * One-shot CTE template: {@code %1$s} = transactions table, {@code %2$s} = accounts table.
     * Splice once at construction (the schema name has already been validated against
     * {@link #SCHEMA_NAME_RE}, so the substitution is safe). All runtime values come in
     * through {@code ?} bind parameters.
     */
    private static final String TRANSFER_SQL_TEMPLATE =
            "WITH params AS (\n" +
                    "  SELECT CAST(? AS VARCHAR(50))  AS tx_id,\n" +
                    "         CAST(? AS NUMERIC)      AS amount,\n" +
                    "         CAST(? AS NUMERIC)      AS src_hold,\n" +
                    "         CAST(? AS NUMERIC)      AS dst_hold,\n" +
                    "         CAST(? AS VARCHAR(255)) AS source,\n" +
                    "         CAST(? AS VARCHAR(255)) AS destination,\n" +
                    "         CAST(? AS VARCHAR(255)) AS asset_id,\n" +
                    "         CAST(? AS VARCHAR(64))  AS asset_type,\n" +
                    "         CAST(? AS VARCHAR(64))  AS action,\n" +
                    "         CAST(? AS JSONB)        AS details\n" +
                    "),\n" +
                    "lock_asset AS (\n" +
                    "  SELECT pg_advisory_xact_lock(hashtext(p.asset_id), hashtext(p.asset_type))\n" +
                    "  FROM params p\n" +
                    "),\n" +
                    "found_tx AS (\n" +
                    "  SELECT t.id, t.asset_id, t.asset_type, t.source, t.destination,\n" +
                    "         t.amount::TEXT AS amount, t.source_held::TEXT AS source_held,\n" +
                    "         t.destination_held::TEXT AS destination_held,\n" +
                    "         t.action, t.details, t.created_at\n" +
                    "  FROM %1$s t, params p, lock_asset l\n" +
                    "  WHERE t.details->>'idempotency_key' = p.details->>'idempotency_key'\n" +
                    "),\n" +
                    "src_upd AS (\n" +
                    "  UPDATE %2$s a\n" +
                    "  SET balance = a.balance - p.amount,\n" +
                    "      held    = a.held + p.src_hold,\n" +
                    "      updated_at = NOW()\n" +
                    "  FROM params p\n" +
                    "  WHERE a.fin_id = p.source\n" +
                    "    AND a.asset_id = p.asset_id\n" +
                    "    AND a.asset_type = p.asset_type\n" +
                    "    AND NOT EXISTS (SELECT 1 FROM found_tx)\n" +
                    "  RETURNING a.fin_id\n" +
                    "),\n" +
                    "dst_upd AS (\n" +
                    "  UPDATE %2$s a\n" +
                    "  SET balance = a.balance + p.amount,\n" +
                    "      held    = a.held + p.dst_hold,\n" +
                    "      updated_at = NOW()\n" +
                    "  FROM params p\n" +
                    "  WHERE a.fin_id = p.destination\n" +
                    "    AND a.asset_id = p.asset_id\n" +
                    "    AND a.asset_type = p.asset_type\n" +
                    "    AND NOT EXISTS (SELECT 1 FROM found_tx)\n" +
                    "  RETURNING a.fin_id\n" +
                    "),\n" +
                    "insert_tx AS (\n" +
                    "  INSERT INTO %1$s\n" +
                    "    (id, asset_id, asset_type, source, destination, amount, source_held, destination_held, action, details)\n" +
                    "  SELECT p.tx_id, p.asset_id, p.asset_type,\n" +
                    "         NULLIF(s.fin_id, ''), NULLIF(d.fin_id, ''),\n" +
                    "         p.amount, p.src_hold, p.dst_hold, p.action, p.details\n" +
                    "  FROM params p\n" +
                    "    LEFT OUTER JOIN src_upd s ON 1=1\n" +
                    "    LEFT OUTER JOIN dst_upd d ON 1=1\n" +
                    "  WHERE NOT EXISTS (SELECT 1 FROM found_tx)\n" +
                    "    AND COALESCE(s.fin_id, '') = p.source\n" +
                    "    AND COALESCE(d.fin_id, '') = p.destination\n" +
                    "  RETURNING id, asset_id, asset_type, source, destination,\n" +
                    "            amount::TEXT AS amount, source_held::TEXT AS source_held,\n" +
                    "            destination_held::TEXT AS destination_held,\n" +
                    "            action, details, created_at\n" +
                    ")\n" +
                    "SELECT * FROM insert_tx\n" +
                    "UNION ALL\n" +
                    "SELECT * FROM found_tx";

    /** Schema this storage is bound to; callers (e.g. AccountMappingService) can use it to
     *  qualify other queries against the same database. */
    public String schemaName() {
        return schema;
    }

    public void ensureAccount(String finId, String assetId) {
        ensureAccount(finId, assetId, DEFAULT_ASSET_TYPE);
    }

    public void ensureAccount(String finId, String assetId, String assetType) {
        dsl.execute(ensureAccountSql, finId, assetId, assetType);
    }

    // ─── Balance ────────────────────────────────────────────────────────────

    public LedgerBalance getBalance(String finId, String assetId) {
        return getBalance(finId, assetId, DEFAULT_ASSET_TYPE);
    }

    public LedgerBalance getBalance(String finId, String assetId, String assetType) {
        Record r = dsl.fetchOne(getBalanceSql, finId, assetId, assetType);
        if (r == null) return LedgerBalance.zero();
        return new LedgerBalance(
                r.get("balance", String.class),
                r.get("held", String.class),
                r.get("available", String.class));
    }

    // ─── Account-level sugar over transfer() ────────────────────────────────

    public LedgerTransaction credit(String finId, String amount, String assetId, LedgerDetails details) {
        return credit(finId, amount, assetId, details, DEFAULT_ASSET_TYPE);
    }

    public LedgerTransaction credit(String finId, String amount, String assetId, LedgerDetails details, String assetType) {
        return transfer("", finId, amount, "0", "0", assetId, assetType, "credit", details);
    }

    public LedgerTransaction debit(String finId, String amount, String assetId, LedgerDetails details) {
        return debit(finId, amount, assetId, details, DEFAULT_ASSET_TYPE);
    }

    public LedgerTransaction debit(String finId, String amount, String assetId, LedgerDetails details, String assetType) {
        return transfer(finId, "", amount, "0", "0", assetId, assetType, "debit", details);
    }

    public LedgerTransaction lock(String finId, String amount, String assetId, LedgerDetails details) {
        return lock(finId, amount, assetId, details, DEFAULT_ASSET_TYPE);
    }

    public LedgerTransaction lock(String finId, String amount, String assetId, LedgerDetails details, String assetType) {
        return transfer(finId, "", "0", amount, "0", assetId, assetType, "lock", details);
    }

    public LedgerTransaction unlock(String finId, String amount, String assetId, LedgerDetails details) {
        return unlock(finId, amount, assetId, details, DEFAULT_ASSET_TYPE);
    }

    public LedgerTransaction unlock(String finId, String amount, String assetId, LedgerDetails details, String assetType) {
        return transfer(finId, "", "0", "-" + amount, "0", assetId, assetType, "unlock", details);
    }

    public LedgerTransaction move(String srcFinId, String dstFinId, String amount, String assetId, LedgerDetails details) {
        return move(srcFinId, dstFinId, amount, assetId, details, DEFAULT_ASSET_TYPE);
    }

    public LedgerTransaction move(String srcFinId, String dstFinId, String amount, String assetId, LedgerDetails details, String assetType) {
        if (srcFinId.equals(dstFinId)) {
            throw new BusinessException(1, "Cannot move from account to itself: " + srcFinId);
        }
        return transfer(srcFinId, dstFinId, amount, "0", "0", assetId, assetType, "move", details);
    }

    public LedgerTransaction unlockAndMove(String srcFinId, String dstFinId, String amount, String assetId, LedgerDetails details) {
        return unlockAndMove(srcFinId, dstFinId, amount, assetId, details, DEFAULT_ASSET_TYPE);
    }

    public LedgerTransaction unlockAndMove(String srcFinId, String dstFinId, String amount, String assetId, LedgerDetails details, String assetType) {
        if (srcFinId.equals(dstFinId)) {
            throw new BusinessException(1, "Cannot move from account to itself: " + srcFinId);
        }
        return transfer(srcFinId, dstFinId, amount, "-" + amount, "0", assetId, assetType, "unlock-and-move", details);
    }

    public LedgerTransaction unlockAndDebit(String finId, String amount, String assetId, LedgerDetails details) {
        return unlockAndDebit(finId, amount, assetId, details, DEFAULT_ASSET_TYPE);
    }

    public LedgerTransaction unlockAndDebit(String finId, String amount, String assetId, LedgerDetails details, String assetType) {
        return transfer(finId, "", amount, "-" + amount, "0", assetId, assetType, "unlock-and-debit", details);
    }

    // ─── Lookups ────────────────────────────────────────────────────────────

    @Nullable
    public LedgerTransaction getTransaction(String txId) {
        Record r = dsl.fetchOne(getTransactionSql, txId);
        return r != null ? toLedgerTransaction(r) : null;
    }

    /**
     * Returns the most-recent transaction tagged with the given {@code operation_id} in its
     * details JSONB, or {@code null} when no row matches.
     *
     * <p>The same {@code operation_id} is intentionally written to multiple ledger rows over a
     * single operation lifecycle — e.g. {@code hold} writes one row, then a later
     * {@code release} / {@code rollback} / held {@code redeem} writes another with the same
     * id. Callers asking for "the receipt for this operation" want the terminating event, so
     * we sort by {@code created_at} descending and return the first hit. {@code id} is used as
     * a tie-breaker so the result is fully deterministic even when two rows land in the same
     * timestamp tick.
     */
    @Nullable
    public LedgerTransaction findByOperationId(String operationId) {
        Record r = dsl.fetchOne(findByOperationIdSql, operationId);
        return r != null ? toLedgerTransaction(r) : null;
    }

    // ─── Distribution queries ───────────────────────────────────────────────

    /**
     * Returns all per-investor accounts with positive balances for an asset, excluding the
     * omnibus account itself. Used to enumerate what would be touched by a bulk flush back into
     * omnibus.
     */
    public java.util.List<DistributedAccount> listDistributedAccounts(
            String omnibusFinId, String assetId, String assetType) {
        return dsl.fetch(listDistributedAccountsSql, assetId, assetType, omnibusFinId)
                .stream()
                .map(r -> new DistributedAccount(
                        r.get("fin_id", String.class),
                        r.get("balance", String.class)))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Returns omnibus + distributed breakdown for an asset, with all arithmetic done in SQL.
     * {@code omnibusBalance} is the conceptual top-line (omnibus row + sum of investor rows),
     * {@code distributed} is the sum of investor balances, {@code available} is what's left on
     * the omnibus row.
     */
    public DistributionTotals getDistributionStatus(
            String omnibusFinId, String assetId, String assetType) {
        Record r = dsl.fetchOne(getDistributionStatusSql,
                omnibusFinId, assetId, assetType,
                assetId, assetType, omnibusFinId);
        if (r == null) {
            return new DistributionTotals("0", "0", "0");
        }
        return new DistributionTotals(
                r.get("omnibus_balance", String.class),
                r.get("distributed", String.class),
                r.get("available", String.class));
    }

    /**
     * Atomically reconcile the omnibus DB row with the supplied on-chain balance.
     *
     * <p>Single UPDATE takes a per-asset advisory lock (same key as
     * {@link #transferSql}, so sync and balance mutations serialize) and computes:
     * {@code target = onChainBalance - SUM(other investor balances)}.
     *
     * <p>If {@code onChainBalance} is less than the already-distributed total, the UPDATE
     * pushes {@code balance} below zero and the {@code CHECK (balance >= 0)} constraint
     * aborts the statement. The caller (service layer) is expected to translate that into
     * a meaningful business error.
     */
    public DistributionTotals syncOmnibusBalance(
            String omnibusFinId, String assetId, String onChainBalance, String assetType) {
        Record r = dsl.fetchOne(syncOmnibusBalanceSql,
                assetId, assetType,           // lock_asset hashtext args
                assetId, assetType, omnibusFinId, // distributed CTE
                onChainBalance,                // SET balance = ?
                omnibusFinId, assetId, assetType); // WHERE
        if (r == null) {
            throw new io.ownera.ledger.adapter.service.BusinessException(1,
                    "syncOmnibusBalance: omnibus account row not found for assetId=" + assetId);
        }
        return new DistributionTotals(
                onChainBalance,
                r.get("distributed", String.class),
                r.get("available", String.class));
    }

    // ─── Atomic CTE transfer ────────────────────────────────────────────────

    /**
     * The one-shot CTE that atomically:
     * <ol>
     *   <li>Takes a per-asset advisory lock so concurrent operations on the same asset serialize.</li>
     *   <li>Looks for an existing transaction with the same idempotency_key; if found, returns it.</li>
     *   <li>Otherwise updates source/destination accounts and inserts the new transaction row.</li>
     * </ol>
     *
     * <p>The {@code CHECK (balance &gt;= 0)} and {@code CHECK (held &lt;= balance)} constraints on
     * {@code accounts} are the safety net: any update that would violate them aborts the whole
     * CTE, which is translated to a {@link BusinessException} here.
     */
    private LedgerTransaction transfer(
            String source,
            String destination,
            String amount,
            String sourceHeld,
            String destHeld,
            String assetId,
            String assetType,
            String action,
            LedgerDetails details) {
        String txId = generateTxId();
        String detailsJson = serializeDetails(details);
        try {
            // detailsJson is passed as a String; the CAST(? AS JSONB) in the SQL turns it into
            // the JSONB the table expects. transferSql is precomputed once per instance with
            // the schema-qualified table names already spliced in (see TRANSFER_SQL_TEMPLATE).
            Result<Record> rows = dsl.fetch(transferSql,
                    txId, amount, sourceHeld, destHeld,
                    source, destination, assetId, assetType,
                    action, detailsJson);
            if (rows.isEmpty()) {
                throw new BusinessException(1,
                        "Transfer failed: account not found for " + Optional.of(source).filter(s -> !s.isEmpty()).orElse(destination));
            }
            return toLedgerTransaction(rows.get(0));
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            String msg = String.valueOf(e.getMessage());
            // accounts_balance_check and accounts_check are the two CHECK constraints in the
            // accounts table; a violation here means the operation would push balance < 0 or
            // held > balance.
            if (msg.contains("accounts_balance_check") || msg.contains("accounts_check")) {
                throw new BusinessException(1,
                        "Insufficient balance for " + action + " on account "
                                + Optional.of(source).filter(s -> !s.isEmpty()).orElse(destination));
            }
            throw e;
        }
    }

    private static LedgerTransaction toLedgerTransaction(Record r) {
        Object detailsRaw = r.get("details");
        LedgerDetails details = parseDetails(detailsRaw);
        Object createdAtRaw = r.get("created_at");
        Instant createdAt;
        if (createdAtRaw instanceof java.sql.Timestamp) {
            createdAt = ((java.sql.Timestamp) createdAtRaw).toInstant();
        } else if (createdAtRaw instanceof java.time.OffsetDateTime) {
            createdAt = ((java.time.OffsetDateTime) createdAtRaw).toInstant();
        } else if (createdAtRaw instanceof Instant) {
            createdAt = (Instant) createdAtRaw;
        } else {
            createdAt = Instant.now();
        }
        return new LedgerTransaction(
                r.get("id", String.class),
                r.get("asset_id", String.class),
                r.get("asset_type", String.class),
                r.get("source", String.class),
                r.get("destination", String.class),
                r.get("amount", String.class),
                r.get("source_held", String.class),
                r.get("destination_held", String.class),
                r.get("action", String.class),
                details,
                createdAt);
    }

    private static LedgerDetails parseDetails(@Nullable Object raw) {
        if (raw == null) {
            return new LedgerDetails("", null, null, null, null);
        }
        String json = raw instanceof JSONB ? ((JSONB) raw).data() : raw.toString();
        try {
            return MAPPER.readValue(json, LedgerDetails.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse transaction details JSON: " + json, e);
        }
    }

    private static String serializeDetails(LedgerDetails details) {
        try {
            return MAPPER.writeValueAsString(details);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize transaction details", e);
        }
    }

    /** Random 32-byte base64url id, matching Node's bs58 length. */
    private static String generateTxId() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
