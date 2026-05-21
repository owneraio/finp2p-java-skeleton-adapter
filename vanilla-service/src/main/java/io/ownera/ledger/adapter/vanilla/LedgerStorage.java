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
    private final String accountsTable;
    private final String transactionsTable;

    public LedgerStorage(DSLContext dsl, String schemaName) {
        if (!SCHEMA_NAME_RE.matcher(schemaName).matches()) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
        this.dsl = dsl;
        this.schema = schemaName;
        this.accountsTable = schemaName + ".accounts";
        this.transactionsTable = schemaName + ".transactions";
    }

    /** Schema this storage is bound to; callers (e.g. AccountMappingService) can use it to
     *  qualify other queries against the same database. */
    public String schemaName() {
        return schema;
    }

    public void ensureAccount(String finId, String assetId) {
        ensureAccount(finId, assetId, DEFAULT_ASSET_TYPE);
    }

    public void ensureAccount(String finId, String assetId, String assetType) {
        dsl.execute(
                "INSERT INTO " + accountsTable + " (fin_id, asset_id, asset_type) " +
                        "VALUES (?, ?, ?) " +
                        "ON CONFLICT (fin_id, asset_id, asset_type) DO NOTHING",
                finId, assetId, assetType);
    }

    // ─── Balance ────────────────────────────────────────────────────────────

    public LedgerBalance getBalance(String finId, String assetId) {
        return getBalance(finId, assetId, DEFAULT_ASSET_TYPE);
    }

    public LedgerBalance getBalance(String finId, String assetId, String assetType) {
        Record r = dsl.fetchOne(
                "SELECT balance::TEXT AS balance, held::TEXT AS held, " +
                        "(balance - held)::TEXT AS available " +
                        "FROM " + accountsTable + " " +
                        "WHERE fin_id = ? AND asset_id = ? AND asset_type = ?",
                finId, assetId, assetType);
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
        Record r = dsl.fetchOne(
                "SELECT id, asset_id, asset_type, source, destination, " +
                        "amount::TEXT AS amount, source_held::TEXT AS source_held, " +
                        "destination_held::TEXT AS destination_held, " +
                        "action, details, created_at " +
                        "FROM " + transactionsTable + " WHERE id = ?",
                txId);
        return r != null ? toLedgerTransaction(r) : null;
    }

    @Nullable
    public LedgerTransaction findByOperationId(String operationId) {
        Record r = dsl.fetchOne(
                "SELECT id, asset_id, asset_type, source, destination, " +
                        "amount::TEXT AS amount, source_held::TEXT AS source_held, " +
                        "destination_held::TEXT AS destination_held, " +
                        "action, details, created_at " +
                        "FROM " + transactionsTable + " WHERE details->>'operation_id' = ?",
                operationId);
        return r != null ? toLedgerTransaction(r) : null;
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
        String sql =
                "WITH params AS (" +
                        "  SELECT CAST(? AS VARCHAR(50))  AS tx_id," +
                        "         CAST(? AS NUMERIC)      AS amount," +
                        "         CAST(? AS NUMERIC)      AS src_hold," +
                        "         CAST(? AS NUMERIC)      AS dst_hold," +
                        "         CAST(? AS VARCHAR(255)) AS source," +
                        "         CAST(? AS VARCHAR(255)) AS destination," +
                        "         CAST(? AS VARCHAR(255)) AS asset_id," +
                        "         CAST(? AS VARCHAR(64))  AS asset_type," +
                        "         CAST(? AS VARCHAR(64))  AS action," +
                        "         CAST(? AS JSONB)        AS details" +
                        ")," +
                        "lock_asset AS (" +
                        "  SELECT pg_advisory_xact_lock(hashtext(p.asset_id), hashtext(p.asset_type))" +
                        "  FROM params p" +
                        ")," +
                        "found_tx AS (" +
                        "  SELECT t.id, t.asset_id, t.asset_type, t.source, t.destination," +
                        "         t.amount::TEXT AS amount, t.source_held::TEXT AS source_held," +
                        "         t.destination_held::TEXT AS destination_held," +
                        "         t.action, t.details, t.created_at" +
                        "  FROM " + transactionsTable + " t, params p, lock_asset l" +
                        "  WHERE t.details->>'idempotency_key' = p.details->>'idempotency_key'" +
                        ")," +
                        "src_upd AS (" +
                        "  UPDATE " + accountsTable + " a" +
                        "  SET balance = a.balance - p.amount," +
                        "      held    = a.held + p.src_hold," +
                        "      updated_at = NOW()" +
                        "  FROM params p" +
                        "  WHERE a.fin_id = p.source" +
                        "    AND a.asset_id = p.asset_id" +
                        "    AND a.asset_type = p.asset_type" +
                        "    AND NOT EXISTS (SELECT 1 FROM found_tx)" +
                        "  RETURNING a.fin_id" +
                        ")," +
                        "dst_upd AS (" +
                        "  UPDATE " + accountsTable + " a" +
                        "  SET balance = a.balance + p.amount," +
                        "      held    = a.held + p.dst_hold," +
                        "      updated_at = NOW()" +
                        "  FROM params p" +
                        "  WHERE a.fin_id = p.destination" +
                        "    AND a.asset_id = p.asset_id" +
                        "    AND a.asset_type = p.asset_type" +
                        "    AND NOT EXISTS (SELECT 1 FROM found_tx)" +
                        "  RETURNING a.fin_id" +
                        ")," +
                        "insert_tx AS (" +
                        "  INSERT INTO " + transactionsTable +
                        "    (id, asset_id, asset_type, source, destination, amount, source_held, destination_held, action, details)" +
                        "  SELECT p.tx_id, p.asset_id, p.asset_type," +
                        "         NULLIF(s.fin_id, ''), NULLIF(d.fin_id, '')," +
                        "         p.amount, p.src_hold, p.dst_hold, p.action, p.details" +
                        "  FROM params p" +
                        "    LEFT OUTER JOIN src_upd s ON 1=1" +
                        "    LEFT OUTER JOIN dst_upd d ON 1=1" +
                        "  WHERE NOT EXISTS (SELECT 1 FROM found_tx)" +
                        "    AND COALESCE(s.fin_id, '') = p.source" +
                        "    AND COALESCE(d.fin_id, '') = p.destination" +
                        "  RETURNING id, asset_id, asset_type, source, destination," +
                        "            amount::TEXT AS amount, source_held::TEXT AS source_held," +
                        "            destination_held::TEXT AS destination_held," +
                        "            action, details, created_at" +
                        ")" +
                        "SELECT * FROM insert_tx" +
                        " UNION ALL " +
                        "SELECT * FROM found_tx";
        try {
            // detailsJson is passed as a String; the CAST(? AS JSONB) in the SQL turns it into
            // the JSONB the table expects.
            Result<Record> rows = dsl.fetch(sql,
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
