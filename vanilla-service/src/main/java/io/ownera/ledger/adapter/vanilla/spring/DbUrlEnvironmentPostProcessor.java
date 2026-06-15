package io.ownera.ledger.adapter.vanilla.spring;

import io.ownera.ledger.adapter.service.PostgresIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Translates the Ownera operator's Go-adapter-style DB env-var contract into the
 * Spring-Boot-native {@code spring.datasource.*} / {@code spring.flyway.*} properties that
 * stock {@code DataSourceAutoConfiguration} expects. Lets a Java adapter deployed against the
 * operator just work — no per-adapter property bridge.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>{@code DB_CONNECTION_STRING} (fallback: {@code LEDGER_DB_CONNECTION_STRING}) —
 *       {@code postgresql://user:pw@host:port/db?params} runtime connection.</li>
 *   <li>{@code MIGRATION_CONNECTION_STRING} (fallback: {@code LEDGER_MIGRATION_CONNECTION_STRING})
 *       — admin role used by Flyway.</li>
 *   <li>{@code LEDGER_SCHEMA} (fallback: {@code LEDGER_SCHEMA_NAME}, default
 *       {@value #DEFAULT_SCHEMA}) — schema that holds the ledger tables. Mapped to
 *       {@code spring.flyway.default-schema}.</li>
 *   <li>{@code LEDGER_DATASOURCE_SEARCH_PATH=true} (default off) — opt-in: inject a per-connection
 *       {@code SET search_path TO &lt;schema&gt;, public} via Hikari's {@code connection-init-sql}.
 *       Leave off for schema-qualified SQL.</li>
 * </ul>
 *
 * <h2>Runtime-role grant</h2>
 *
 * <p>The skeleton ships an {@code R__grant_ledger_user.sql} repeatable migration that grants
 * schema + table privileges to the runtime role. The migration runs as the admin role (from
 * {@code MIGRATION_CONNECTION_STRING}) and needs to know which runtime role to grant access to,
 * via the Flyway placeholder {@code ${ledger_user}}. This EPP feeds the username extracted from
 * {@code DB_CONNECTION_STRING} into {@code spring.flyway.placeholders.ledger_user} so the grant
 * migration applies automatically — without this bridge the placeholder is empty, the migration
 * is a silent no-op, and the runtime app gets {@code "permission denied for schema"} on first
 * DB hit.
 *
 * <h2>Precedence</h2>
 *
 * <p>Runs as an {@link EnvironmentPostProcessor}, so it executes <em>before</em> Spring Boot's
 * {@code application.yml} is loaded onto the {@code Environment}. Properties this EPP installs
 * therefore win over yaml without any explicit check — but it still backs off when a
 * higher-precedence source (command-line, system properties, system env exporting
 * {@code SPRING_DATASOURCE_URL} directly) has already supplied the equivalent Spring property.
 * This preserves manual overrides for test profiles, dev runs, and Testcontainers.
 *
 * <h2>Userinfo stripping</h2>
 *
 * <p>The Postgres JDBC driver does <strong>not</strong> parse {@code user:pw@host} from
 * {@code jdbc:postgresql://} URLs — it treats the whole {@code user:pw@host} as the host and
 * fails with {@code UnknownHostException}. Credentials are extracted into separate
 * {@code spring.datasource.username} / {@code .password} and stripped from the URL before
 * setting {@code spring.datasource.url}.
 */
public class DbUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DbUrlEnvironmentPostProcessor.class);

    public static final String DB_CONNECTION_STRING = "DB_CONNECTION_STRING";
    public static final String LEGACY_DB_CONNECTION_STRING = "LEDGER_DB_CONNECTION_STRING";
    public static final String MIGRATION_CONNECTION_STRING = "MIGRATION_CONNECTION_STRING";
    public static final String LEGACY_MIGRATION_CONNECTION_STRING = "LEDGER_MIGRATION_CONNECTION_STRING";
    public static final String LEDGER_SCHEMA = "LEDGER_SCHEMA";
    public static final String LEGACY_LEDGER_SCHEMA = "LEDGER_SCHEMA_NAME";
    public static final String DEFAULT_SCHEMA = "ledger_adapter";
    public static final String ENABLE_SEARCH_PATH = "LEDGER_DATASOURCE_SEARCH_PATH";

    static final String PROPERTY_SOURCE_NAME = "owneraDbConnectionBridge";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        Map<String, Object> derived = new HashMap<>();

        // Runtime datasource.
        String runtimeUrl = firstNonBlank(env.getProperty(DB_CONNECTION_STRING), env.getProperty(LEGACY_DB_CONNECTION_STRING));
        if (runtimeUrl != null && !isOverriddenByHigherPrecedenceSource(env, "spring.datasource.url")) {
            try {
                Parsed parsed = parse(runtimeUrl);
                derived.put("spring.datasource.url", parsed.jdbcUrl);
                if (parsed.username != null) {
                    derived.put("spring.datasource.username", parsed.username);
                    // Feed the runtime username into the Flyway placeholder consumed by the
                    // skeleton's R__grant_ledger_user.sql. The grant migration runs as the
                    // admin role (MIGRATION_CONNECTION_STRING) and needs to know which runtime
                    // role to grant access to. Without this bridge the placeholder is empty,
                    // the migration silently no-ops, and the runtime app gets "permission
                    // denied for schema" on first DB hit.
                    if (!isOverriddenByHigherPrecedenceSource(env, "spring.flyway.placeholders.ledger_user")) {
                        derived.put("spring.flyway.placeholders.ledger_user", parsed.username);
                    }
                }
                if (parsed.password != null) derived.put("spring.datasource.password", parsed.password);
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to parse DB_CONNECTION_STRING; leaving spring.datasource.* untouched: {}", e.getMessage());
            }
        }

        // Flyway migration datasource.
        String migrationUrl = firstNonBlank(env.getProperty(MIGRATION_CONNECTION_STRING), env.getProperty(LEGACY_MIGRATION_CONNECTION_STRING));
        if (migrationUrl != null && !isOverriddenByHigherPrecedenceSource(env, "spring.flyway.url")) {
            try {
                Parsed parsed = parse(migrationUrl);
                derived.put("spring.flyway.url", parsed.jdbcUrl);
                if (parsed.username != null) derived.put("spring.flyway.user", parsed.username);
                if (parsed.password != null) derived.put("spring.flyway.password", parsed.password);
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to parse MIGRATION_CONNECTION_STRING; leaving spring.flyway.* untouched: {}", e.getMessage());
            }
        }

        // Schema → Flyway's default-schema (and optional runtime search_path).
        // Only install when we've already installed a DB URL — otherwise this EPP would leak a
        // schema setting into adapters that don't use a DB at all (the no-op case must stay
        // truly no-op so vanilla-service stays compatible with bare / observer-mode deployments).
        if (!derived.isEmpty()) {
            // Resolution chain: explicit LEDGER_SCHEMA wins, then the legacy alias, then a
            // schema derived from the pod's HOSTNAME (sanitised through PostgresIdentifier so
            // k8s-style hyphenated names like `swift-rails-0` map cleanly to `swift_rails_0`),
            // then the static framework default. Sanitising in code keeps adapter operators
            // from having to think about Postgres' identifier grammar at the env-var layer.
            String schema = firstNonBlank(env.getProperty(LEDGER_SCHEMA), env.getProperty(LEGACY_LEDGER_SCHEMA));
            if (schema == null) {
                String hostname = env.getProperty("HOSTNAME");
                if (hostname != null && !hostname.isEmpty()) {
                    try {
                        schema = PostgresIdentifier.coerce(hostname);
                    } catch (IllegalArgumentException e) {
                        logger.warn("HOSTNAME={} could not be coerced to a Postgres identifier; falling back to default schema '{}': {}",
                                hostname, DEFAULT_SCHEMA, e.getMessage());
                    }
                }
            }
            if (schema == null) schema = DEFAULT_SCHEMA;
            if (!isOverriddenByHigherPrecedenceSource(env, "spring.flyway.default-schema")) {
                derived.put("spring.flyway.default-schema", schema);
            }
            if (Boolean.parseBoolean(env.getProperty(ENABLE_SEARCH_PATH))
                    && !isOverriddenByHigherPrecedenceSource(env, "spring.datasource.hikari.connection-init-sql")) {
                derived.put("spring.datasource.hikari.connection-init-sql", "SET search_path TO " + schema + ", public");
            }
        }

        if (derived.isEmpty()) {
            return;
        }

        MutablePropertySources sources = env.getPropertySources();
        sources.addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, derived));
        logger.info("Installed {} property(s) from Ownera DB env-var bridge: {}", derived.size(), derived.keySet());
    }

    /**
     * Parses {@code postgresql://user:pw@host:port/db?params} (with or without a leading
     * {@code jdbc:}) into a JDBC URL plus extracted credentials.
     */
    static Parsed parse(String connectionString) {
        String working = connectionString.startsWith("jdbc:") ? connectionString.substring(5) : connectionString;
        URI uri;
        try {
            uri = new URI(working);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Not a valid URI: " + redactedPreview(connectionString), e);
        }

        String username = null;
        String password = null;
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null && !userInfo.isEmpty()) {
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                username = decode(userInfo.substring(0, colon));
                password = decode(userInfo.substring(colon + 1));
            } else {
                username = decode(userInfo);
            }
        }

        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("Connection string is missing a host: " + redactedPreview(connectionString));
        }
        // Bracket IPv6 hosts when reconstructing; URI.getHost behaviour varies across JDK
        // versions (sometimes returns the bracketed form, sometimes the bare address), so check
        // before adding to avoid double-bracketing.
        String hostForJdbc = (host.contains(":") && !host.startsWith("[")) ? "[" + host + "]" : host;

        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(hostForJdbc);
        if (uri.getPort() > 0) {
            jdbc.append(':').append(uri.getPort());
        }
        if (uri.getRawPath() != null && !uri.getRawPath().isEmpty()) {
            jdbc.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
            jdbc.append('?').append(uri.getRawQuery());
        }
        return new Parsed(jdbc.toString(), username, password);
    }

    /**
     * Higher-precedence sources are command-line args, JVM system properties, and the OS
     * environment (which is how an operator deployment would supply {@code SPRING_DATASOURCE_URL}
     * directly, bypassing this bridge). {@code application.yml} and dynamically-added
     * sources are loaded later — they don't count here, so this EPP wins over yaml.
     */
    private static boolean isOverriddenByHigherPrecedenceSource(ConfigurableEnvironment env, String key) {
        for (PropertySource<?> source : env.getPropertySources()) {
            if (isHigherPrecedence(source.getName()) && source.containsProperty(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHigherPrecedence(String sourceName) {
        // Spring Boot's well-known source names: systemProperties, systemEnvironment,
        // commandLineArgs. Match defensively in case Spring renames them.
        return sourceName != null
                && (sourceName.contains("systemProperties")
                || sourceName.contains("systemEnvironment")
                || sourceName.contains("commandLine"));
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    private static String decode(String raw) {
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    /** Redact userinfo before logging — credentials must not hit the logs even on parse failure. */
    private static String redactedPreview(String connectionString) {
        int at = connectionString.indexOf('@');
        int slashSlash = connectionString.indexOf("//");
        if (at > 0 && slashSlash > 0 && slashSlash < at) {
            return connectionString.substring(0, slashSlash + 2) + "***@" + connectionString.substring(at + 1);
        }
        return connectionString;
    }

    static final class Parsed {
        final String jdbcUrl;
        final String username;
        final String password;

        Parsed(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }
    }
}
