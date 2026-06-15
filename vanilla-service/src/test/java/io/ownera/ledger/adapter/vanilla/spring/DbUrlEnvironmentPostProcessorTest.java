package io.ownera.ledger.adapter.vanilla.spring;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the Ownera operator → Spring Boot DB property bridge. Covers both the URL parser in
 * isolation and the {@link DbUrlEnvironmentPostProcessor#postProcessEnvironment} end-to-end
 * behaviour (back-off precedence, alias fallback, search-path opt-in, no-op on missing env).
 */
class DbUrlEnvironmentPostProcessorTest {

    private final DbUrlEnvironmentPostProcessor epp = new DbUrlEnvironmentPostProcessor();

    // ---- URL parser ----

    @Test
    void parseSplitsUserinfoOutOfTheJdbcUrl() {
        DbUrlEnvironmentPostProcessor.Parsed p = DbUrlEnvironmentPostProcessor.parse(
                "postgresql://ledger:s3cret@db.example.com:5432/finp2p");
        assertEquals("jdbc:postgresql://db.example.com:5432/finp2p", p.jdbcUrl);
        assertEquals("ledger", p.username);
        assertEquals("s3cret", p.password);
    }

    @Test
    void parsePreservesQueryParamsVerbatim() {
        DbUrlEnvironmentPostProcessor.Parsed p = DbUrlEnvironmentPostProcessor.parse(
                "postgresql://u:p@h:5432/d?sslmode=require&ApplicationName=swift-rails");
        assertEquals("jdbc:postgresql://h:5432/d?sslmode=require&ApplicationName=swift-rails", p.jdbcUrl);
    }

    @Test
    void parseHandlesUrlEncodedPassword() {
        // operator may URL-encode special chars in the password (e.g. '@' → %40).
        DbUrlEnvironmentPostProcessor.Parsed p = DbUrlEnvironmentPostProcessor.parse(
                "postgresql://user:p%40ss%21@host:5432/db");
        assertEquals("user", p.username);
        assertEquals("p@ss!", p.password);
        assertFalse(p.jdbcUrl.contains("user:"), "userinfo must be stripped from the JDBC URL");
    }

    @Test
    void parseHandlesUserWithoutPassword() {
        DbUrlEnvironmentPostProcessor.Parsed p = DbUrlEnvironmentPostProcessor.parse(
                "postgresql://trusted-user@host:5432/db");
        assertEquals("trusted-user", p.username);
        assertNull(p.password);
    }

    @Test
    void parseHandlesIpv6HostByReAddingBrackets() {
        // URI.getHost strips brackets; we must reinsert them so the JDBC driver accepts the URL.
        DbUrlEnvironmentPostProcessor.Parsed p = DbUrlEnvironmentPostProcessor.parse(
                "postgresql://u:p@[::1]:5432/db");
        assertEquals("jdbc:postgresql://[::1]:5432/db", p.jdbcUrl);
    }

    @Test
    void parseAcceptsAlreadyJdbcPrefixedInput() {
        // Defensive: if someone hands us a jdbc-prefixed URL, don't double-prefix.
        DbUrlEnvironmentPostProcessor.Parsed p = DbUrlEnvironmentPostProcessor.parse(
                "jdbc:postgresql://u:p@host:5432/db?sslmode=disable");
        assertEquals("jdbc:postgresql://host:5432/db?sslmode=disable", p.jdbcUrl);
        assertEquals("u", p.username);
        assertEquals("p", p.password);
    }

    @Test
    void parseTolaratesMissingPortAndDb() {
        DbUrlEnvironmentPostProcessor.Parsed p = DbUrlEnvironmentPostProcessor.parse(
                "postgresql://u:p@host");
        assertEquals("jdbc:postgresql://host", p.jdbcUrl);
    }

    // ---- EPP end-to-end ----

    @Test
    void installsDatasourceAndFlywayPropertiesFromOperatorEnvVars() {
        StandardEnvironment env = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://runtime:rpw@db:5432/finp2p?sslmode=require",
                "MIGRATION_CONNECTION_STRING", "postgresql://admin:apw@db:5432/finp2p"
        )));

        epp.postProcessEnvironment(env, null);

        assertEquals("jdbc:postgresql://db:5432/finp2p?sslmode=require", env.getProperty("spring.datasource.url"));
        assertEquals("runtime", env.getProperty("spring.datasource.username"));
        assertEquals("rpw", env.getProperty("spring.datasource.password"));
        assertEquals("jdbc:postgresql://db:5432/finp2p", env.getProperty("spring.flyway.url"));
        assertEquals("admin", env.getProperty("spring.flyway.user"));
        assertEquals("apw", env.getProperty("spring.flyway.password"));
        // Default schema applies when neither LEDGER_SCHEMA nor the legacy alias is set.
        assertEquals(DbUrlEnvironmentPostProcessor.DEFAULT_SCHEMA, env.getProperty("spring.flyway.default-schema"));
    }

    @Test
    void bridgesRuntimeUsernameIntoFlywayLedgerUserPlaceholder() {
        // The skeleton's R__grant_ledger_user.sql repeatable migration grants schema/table
        // privileges to ${ledger_user}, which is sourced from spring.flyway.placeholders.ledger_user.
        // Without this bridge the placeholder stays empty and the migration silently no-ops —
        // runtime app then gets "permission denied for schema" on first DB hit.
        StandardEnvironment env = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://ledger:rpw@db:5432/finp2p",
                "MIGRATION_CONNECTION_STRING", "postgresql://migration:apw@db:5432/finp2p"
        )));

        epp.postProcessEnvironment(env, null);

        assertEquals("ledger", env.getProperty("spring.datasource.username"));
        assertEquals("ledger", env.getProperty("spring.flyway.placeholders.ledger_user"),
                "runtime username must be mirrored into the Flyway placeholder feeding R__grant_ledger_user.sql");
    }

    @Test
    void ledgerUserPlaceholderBacksOffWhenExplicitlyOverridden() {
        // An adapter that wants the grant migration to target a different role than the
        // runtime user (e.g. role-separation deployment with a shared GRANTed group) can
        // override the placeholder explicitly — the bridge respects that.
        StandardEnvironment env = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://ledger:rpw@db:5432/finp2p",
                "SPRING_FLYWAY_PLACEHOLDERS_LEDGER_USER", "shared_readers"
        )));

        epp.postProcessEnvironment(env, null);

        assertEquals("ledger", env.getProperty("spring.datasource.username"));
        assertEquals("shared_readers", env.getProperty("spring.flyway.placeholders.ledger_user"));
    }

    @Test
    void ledgerUserPlaceholderIsAbsentWhenConnectionStringHasNoUserinfo() {
        // A trust-auth deployment may use a connection string with no embedded credentials.
        // In that case there's no runtime username to bridge — the placeholder stays unset
        // and the grant migration becomes a deliberate no-op rather than mis-targeting.
        StandardEnvironment env = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://db:5432/finp2p"
        )));

        epp.postProcessEnvironment(env, null);

        assertNull(env.getProperty("spring.datasource.username"));
        assertNull(env.getProperty("spring.flyway.placeholders.ledger_user"));
    }

    @Test
    void readsLegacyConnectionStringAliasWhenCanonicalNameIsUnset() {
        // Older operator deployments and .env-based dev still use the LEDGER_-prefixed form.
        StandardEnvironment env = environmentWith(systemEnv(Map.of(
                "LEDGER_DB_CONNECTION_STRING", "postgresql://u:p@host:5432/db",
                "LEDGER_MIGRATION_CONNECTION_STRING", "postgresql://u:p@host:5432/db"
        )));

        epp.postProcessEnvironment(env, null);

        assertEquals("jdbc:postgresql://host:5432/db", env.getProperty("spring.datasource.url"));
        assertEquals("jdbc:postgresql://host:5432/db", env.getProperty("spring.flyway.url"));
    }

    @Test
    void canonicalConnectionStringTakesPrecedenceOverLegacyAlias() {
        StandardEnvironment env = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://canon:c@canon-host:5432/d",
                "LEDGER_DB_CONNECTION_STRING", "postgresql://legacy:l@legacy-host:5432/d"
        )));

        epp.postProcessEnvironment(env, null);

        assertEquals("jdbc:postgresql://canon-host:5432/d", env.getProperty("spring.datasource.url"));
    }

    @Test
    void readsLedgerSchemaAndItsLegacyAlias() {
        StandardEnvironment canonical = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://u:p@h:5432/d",
                "LEDGER_SCHEMA", "canonical_schema"
        )));
        epp.postProcessEnvironment(canonical, null);
        assertEquals("canonical_schema", canonical.getProperty("spring.flyway.default-schema"));

        StandardEnvironment legacy = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://u:p@h:5432/d",
                "LEDGER_SCHEMA_NAME", "swift_rails"
        )));
        epp.postProcessEnvironment(legacy, null);
        assertEquals("swift_rails", legacy.getProperty("spring.flyway.default-schema"));
    }

    @Test
    void defaultSchemaIsDerivedFromHostnameWhenNoExplicitSchemaIsSet() {
        // No LEDGER_SCHEMA / LEDGER_SCHEMA_NAME → fall through to HOSTNAME, sanitised through
        // PostgresIdentifier so the kubernetes-pod naming convention (lower-case + hyphens)
        // becomes a valid Postgres identifier without operator hand-coding.
        StandardEnvironment env = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://u:p@h:5432/d",
                "HOSTNAME", "swift-rails-0"
        )));

        epp.postProcessEnvironment(env, null);

        assertEquals("swift_rails_0", env.getProperty("spring.flyway.default-schema"),
                "schema must derive from HOSTNAME, sanitised via PostgresIdentifier.coerce");
    }

    @Test
    void explicitLedgerSchemaWinsOverHostnameFallback() {
        // Sanity: if the operator wants a shared schema across replicas they set
        // LEDGER_SCHEMA explicitly; that must beat the HOSTNAME-derived fallback.
        StandardEnvironment env = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://u:p@h:5432/d",
                "LEDGER_SCHEMA", "shared",
                "HOSTNAME", "swift-rails-0"
        )));

        epp.postProcessEnvironment(env, null);

        assertEquals("shared", env.getProperty("spring.flyway.default-schema"));
    }

    @Test
    void hostnameFallbackOnlyKicksInWhenBothSchemaAliasesAreUnset() {
        // LEGACY alias still wins over HOSTNAME (matches the alias precedence we documented).
        StandardEnvironment env = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://u:p@h:5432/d",
                "LEDGER_SCHEMA_NAME", "legacy_named",
                "HOSTNAME", "swift-rails-0"
        )));

        epp.postProcessEnvironment(env, null);

        assertEquals("legacy_named", env.getProperty("spring.flyway.default-schema"));
    }

    @Test
    void constantDefaultIsUsedWhenHostnameIsAlsoUnset() {
        // When neither schema env var nor HOSTNAME is set, fall back to the framework constant.
        // This is the last-resort path for non-containerised dev / fat-jar runs without an env.
        StandardEnvironment env = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://u:p@h:5432/d"
                // no HOSTNAME, no LEDGER_SCHEMA*
        )));

        epp.postProcessEnvironment(env, null);

        assertEquals(DbUrlEnvironmentPostProcessor.DEFAULT_SCHEMA,
                env.getProperty("spring.flyway.default-schema"));
    }

    @Test
    void searchPathInjectionIsOptInAndDefaultsOff() {
        StandardEnvironment off = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://u:p@h:5432/d",
                "LEDGER_SCHEMA", "my_schema"
        )));
        epp.postProcessEnvironment(off, null);
        assertNull(off.getProperty("spring.datasource.hikari.connection-init-sql"),
                "default-off: schema-qualified SQL doesn't need a search-path");

        StandardEnvironment on = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://u:p@h:5432/d",
                "LEDGER_SCHEMA", "my_schema",
                "LEDGER_DATASOURCE_SEARCH_PATH", "true"
        )));
        epp.postProcessEnvironment(on, null);
        assertEquals("SET search_path TO my_schema, public",
                on.getProperty("spring.datasource.hikari.connection-init-sql"));
    }

    @Test
    void backsOffWhenSpringDatasourceUrlIsAlreadyOnSystemEnv() {
        // Equivalent of someone exporting SPRING_DATASOURCE_URL=... directly. Stock Spring
        // autoconfig should handle it without our bridge.
        StandardEnvironment env = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://bridge:bp@bridge-host:5432/d",
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://explicit-host:5432/d",
                "SPRING_DATASOURCE_USERNAME", "explicit-user"
        )));

        epp.postProcessEnvironment(env, null);

        // EPP leaves the explicit env value alone.
        assertEquals("jdbc:postgresql://explicit-host:5432/d", env.getProperty("spring.datasource.url"));
        assertEquals("explicit-user", env.getProperty("spring.datasource.username"));
    }

    @Test
    void backsOffWhenSpringDatasourceUrlIsAlreadyOnSystemProperties() {
        // -Dspring.datasource.url=... at JVM startup must beat the bridge.
        Map<String, Object> sysProps = new HashMap<>();
        sysProps.put("spring.datasource.url", "jdbc:postgresql://override-host:5432/d");

        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("systemProperties", sysProps));
        env.getPropertySources().addLast(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "postgresql://bridge:bp@bridge-host:5432/d"
        )));

        epp.postProcessEnvironment(env, null);

        assertEquals("jdbc:postgresql://override-host:5432/d", env.getProperty("spring.datasource.url"));
    }

    @Test
    void noOpsGracefullyWhenNoConnectionEnvVarsAreSet() {
        // Tests / Testcontainers / local dev with no operator-style env vars must not see this
        // EPP install a half-baked property source. spring.datasource.* must remain absent.
        StandardEnvironment env = environmentWith(systemEnv(Map.of()));

        epp.postProcessEnvironment(env, null);

        assertNull(env.getProperty("spring.datasource.url"));
        assertNull(env.getProperty("spring.datasource.username"));
        assertNull(env.getProperty("spring.flyway.url"));
        // Even spring.flyway.default-schema doesn't get installed without a DB connection — there
        // is nothing for Flyway to migrate against.
        assertFalse(env.getPropertySources().contains(DbUrlEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
    }

    @Test
    void invalidConnectionStringWarnsAndLeavesPropertiesUnset() {
        StandardEnvironment env = environmentWith(systemEnv(Map.of(
                "DB_CONNECTION_STRING", "not a url at all"
        )));

        // Should not throw — parse failure is contained.
        epp.postProcessEnvironment(env, null);

        assertNull(env.getProperty("spring.datasource.url"),
                "parse failure must leave spring.datasource.* untouched, not install garbage");
    }

    // ---- helpers ----

    private static StandardEnvironment environmentWith(SystemEnvironmentPropertySource sysEnv) {
        StandardEnvironment env = new StandardEnvironment();
        // Replace the auto-discovered system env source (which would pollute the test with the
        // host's real environment) with our controlled one.
        env.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, sysEnv);
        return env;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static SystemEnvironmentPropertySource systemEnv(Map<String, String> entries) {
        Map<String, Object> copy = new HashMap<>(entries);
        return new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                (Map) copy);
    }
}
