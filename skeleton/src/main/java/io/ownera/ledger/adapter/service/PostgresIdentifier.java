package io.ownera.ledger.adapter.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helpers for working with Postgres identifiers (schema, table, column names) that get spliced
 * straight into SQL — Postgres parameterised queries can't bind identifiers, so any name we
 * interpolate needs to be locked down at the source.
 *
 * <p>Two operations:
 * <ul>
 *   <li>{@link #assertValid(String)} — strict validator; throws on anything that isn't a valid
 *       unquoted Postgres identifier within the framework's size cap.</li>
 *   <li>{@link #coerce(String)} — defensive sanitiser; coerces an arbitrary input (e.g. a
 *       kubernetes pod hostname like {@code swift-rails-0}) into a value that satisfies
 *       {@link #assertValid}. Non-{@code [A-Za-z0-9_]} characters become {@code _};
 *       leading-digit inputs get a {@code _} prefix; over-long inputs are truncated and given
 *       a deterministic MD5 suffix so distinct long inputs that share a prefix don't collapse
 *       onto the same identifier.</li>
 * </ul>
 *
 * <p>Behaviour mirrors the Node.js skeleton's {@code toPostgresIdentifier} /
 * {@code assertValidPostgresIdentifier} pair so both implementations derive the same names from
 * the same inputs.
 */
public final class PostgresIdentifier {

    /**
     * Max byte length we allow for any identifier we splice into SQL. Postgres' raw limit is
     * 63 bytes (NAMEDATALEN − 1), but it auto-derives suffixed identifiers from table names
     * ({@code <table>_pkey}, {@code <table>_<col>_fkey}, {@code <table>_<col>_seq}, …); 50
     * bytes leaves ~13 bytes of headroom so those derivatives don't silently truncate with a
     * server-side NOTICE.
     */
    public static final int MAX_LENGTH = 50;

    private PostgresIdentifier() {}

    /**
     * @throws IllegalArgumentException if {@code name} is null, doesn't match the unquoted
     *     Postgres identifier grammar, or exceeds {@value MAX_LENGTH} bytes.
     */
    public static void assertValid(String name) {
        if (name == null || !name.matches("^[A-Za-z_][A-Za-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid Postgres identifier: " + asLiteral(name)
                    + ". Must match /^[A-Za-z_][A-Za-z0-9_]*$/ (ASCII letter or underscore, "
                    + "then letters/digits/underscores).");
        }
        int byteLength = name.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength > MAX_LENGTH) {
            throw new IllegalArgumentException("Invalid Postgres identifier: " + asLiteral(name)
                    + " is " + byteLength + " bytes; capped at " + MAX_LENGTH
                    + " bytes to leave headroom for Postgres auto-derived identifiers "
                    + "(e.g. <name>_pkey, <name>_<col>_fkey, <name>_<col>_seq) within the "
                    + "63-byte NAMEDATALEN limit.");
        }
    }

    /**
     * Coerce an arbitrary input string into a value that satisfies {@link #assertValid}.
     *
     * <p>Algorithm (matches Node's {@code toPostgresIdentifier}):
     * <ol>
     *   <li>Replace every non-{@code [A-Za-z0-9_]} character with {@code _}.</li>
     *   <li>If the result starts with a digit, prefix it with {@code _}.</li>
     *   <li>If the result exceeds {@value MAX_LENGTH} bytes, truncate to leave room for an
     *       8-char MD5-derived suffix (from the <em>original</em> input — so two long inputs
     *       sharing a prefix don't collapse to the same identifier) joined with {@code _}.</li>
     * </ol>
     *
     * @throws IllegalArgumentException if {@code raw} is null or empty.
     */
    public static String coerce(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("Cannot derive a Postgres identifier from a null or empty string.");
        }
        String sanitized = raw.replaceAll("[^A-Za-z0-9_]", "_");
        if (Character.isDigit(sanitized.charAt(0))) {
            sanitized = "_" + sanitized;
        }
        if (sanitized.getBytes(StandardCharsets.UTF_8).length > MAX_LENGTH) {
            int hashSuffixLength = 8;
            int prefixLength = MAX_LENGTH - hashSuffixLength - 1;
            String suffix = md5Hex(raw).substring(0, hashSuffixLength);
            sanitized = sanitized.substring(0, prefixLength) + "_" + suffix;
        }
        assertValid(sanitized);
        return sanitized;
    }

    private static String md5Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 is part of every Java SE distribution; this is unreachable.
            throw new IllegalStateException("MD5 unavailable on this JVM", e);
        }
    }

    private static String asLiteral(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
