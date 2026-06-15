package io.ownera.ledger.adapter.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mirrors the Node.js skeleton's coverage of {@code toPostgresIdentifier} /
 * {@code assertValidPostgresIdentifier} so the two implementations stay aligned. Adding /
 * removing tests here means doing the same on the Node side (and vice versa).
 */
class PostgresIdentifierTest {

    // ── coerce: passthrough ─────────────────────────────────────────────

    @Test
    void coercePassesThroughAlreadyValidIdentifiers() {
        assertEquals("ledger_adapter", PostgresIdentifier.coerce("ledger_adapter"));
        assertEquals("_underscore", PostgresIdentifier.coerce("_underscore"));
        assertEquals("swift_rails_0", PostgresIdentifier.coerce("swift_rails_0"));
    }

    // ── coerce: character sanitisation ──────────────────────────────────

    @Test
    void coerceReplacesDisallowedCharactersWithUnderscores() {
        // Kubernetes-style hostnames are the primary motivation — pods like swift-rails-0 must
        // sanitise cleanly without operator intervention.
        assertEquals("swift_rails_0", PostgresIdentifier.coerce("swift-rails-0"));
        assertEquals("sepolia_mainnet", PostgresIdentifier.coerce("sepolia-mainnet"));
        assertEquals("ada_pter_id", PostgresIdentifier.coerce("ada.pter id"));
        assertEquals("h_llo", PostgresIdentifier.coerce("héllo"));   // non-ASCII → _
    }

    // ── coerce: leading-digit guard ─────────────────────────────────────

    @Test
    void coercePrefixesUnderscoreWhenResultWouldStartWithADigit() {
        assertEquals("_1adapter", PostgresIdentifier.coerce("1adapter"));
        assertEquals("_0", PostgresIdentifier.coerce("0"));
        // After non-ASCII sanitisation, the result still leads with a digit → prefix kicks in.
        assertEquals("_9_pod", PostgresIdentifier.coerce("9-pod"));
    }

    // ── coerce: length cap + md5 suffix ─────────────────────────────────

    @Test
    void coerceTruncatesAndAppendsMd5SuffixWhenInputExceedsCap() {
        String longRaw = "a".repeat(80);   // 80 underscore-safe chars, well above the 50 cap
        String result = PostgresIdentifier.coerce(longRaw);
        assertEquals(PostgresIdentifier.MAX_LENGTH, result.length(),
                "truncated result must hit the cap exactly");
        assertDoesNotThrow(() -> PostgresIdentifier.assertValid(result),
                "truncated result must still satisfy assertValid");
        // The suffix is the first 8 chars of md5(raw), joined by an underscore. Two different
        // long inputs sharing a prefix must NOT collapse to the same identifier — that's the
        // whole point of the md5 suffix.
        String alt = ("a".repeat(79)) + "b";   // 80 chars, same 79-prefix
        String altResult = PostgresIdentifier.coerce(alt);
        assertEquals(PostgresIdentifier.MAX_LENGTH, altResult.length());
        assertTrue(!result.equals(altResult),
                "distinct long inputs must yield distinct coerced identifiers");
    }

    // ── coerce: input validation ────────────────────────────────────────

    @Test
    void coerceRejectsNullAndEmpty() {
        assertThrows(IllegalArgumentException.class, () -> PostgresIdentifier.coerce(null));
        assertThrows(IllegalArgumentException.class, () -> PostgresIdentifier.coerce(""));
    }

    // ── assertValid: positive cases ─────────────────────────────────────

    @Test
    void assertValidAcceptsValidIdentifiers() {
        assertDoesNotThrow(() -> PostgresIdentifier.assertValid("ledger_adapter"));
        assertDoesNotThrow(() -> PostgresIdentifier.assertValid("a"));
        assertDoesNotThrow(() -> PostgresIdentifier.assertValid("_a"));
        assertDoesNotThrow(() -> PostgresIdentifier.assertValid("a_1"));
    }

    // ── assertValid: rejection cases ────────────────────────────────────

    @Test
    void assertValidRejectsLeadingDigit() {
        assertThrows(IllegalArgumentException.class, () -> PostgresIdentifier.assertValid("1adapter"));
    }

    @Test
    void assertValidRejectsDisallowedCharacters() {
        assertThrows(IllegalArgumentException.class, () -> PostgresIdentifier.assertValid("swift-rails"));
        assertThrows(IllegalArgumentException.class, () -> PostgresIdentifier.assertValid("a b"));
        assertThrows(IllegalArgumentException.class, () -> PostgresIdentifier.assertValid("héllo"));
    }

    @Test
    void assertValidRejectsNullAndEmpty() {
        assertThrows(IllegalArgumentException.class, () -> PostgresIdentifier.assertValid(null));
        assertThrows(IllegalArgumentException.class, () -> PostgresIdentifier.assertValid(""));
    }

    @Test
    void assertValidRejectsOverlongIdentifiers() {
        String tooLong = "a".repeat(PostgresIdentifier.MAX_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> PostgresIdentifier.assertValid(tooLong));
    }
}
