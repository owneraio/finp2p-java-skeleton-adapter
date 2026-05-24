package io.ownera.ledger.adapter.vanilla.spring;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins down EPP discovery registration. We ship <em>two</em> files because Spring Boot's
 * {@code EnvironmentPostProcessor} extension point is read differently across versions:
 *
 * <ul>
 *   <li>Boot 2.7 reads <em>only</em> {@code META-INF/spring.factories} for this extension point;
 *       the {@code .imports}-style discovery doesn't apply.</li>
 *   <li>Boot 3.x reads
 *       {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports}
 *       (and tolerates {@code spring.factories} as a deprecated fallback).</li>
 * </ul>
 *
 * <p>The 0.28.13 release shipped only the Boot-3 file, which silently broke EPP discovery on
 * Boot 2.7 consumers (Hikari ended up with a null URL, no log line, no error). These tests
 * fail loudly if either file goes missing or stops referencing the EPP — so the regression
 * can't slip through again.
 */
class DbUrlEnvironmentPostProcessorDiscoveryTest {

    private static final String EPP_FQN = "io.ownera.ledger.adapter.vanilla.spring.DbUrlEnvironmentPostProcessor";

    @Test
    void boot27DiscoveryViaSpringFactoriesIsRegistered() throws IOException {
        URL resource = currentClassLoader().getResource("META-INF/spring.factories");
        assertNotNull(resource, "META-INF/spring.factories must ship in the JAR — Boot 2.7 needs it");

        Properties props = new Properties();
        try (InputStream in = resource.openStream()) {
            props.load(in);
        }
        String value = props.getProperty("org.springframework.boot.env.EnvironmentPostProcessor");
        assertNotNull(value, "EnvironmentPostProcessor key missing in spring.factories");
        assertTrue(value.contains(EPP_FQN),
                "spring.factories must reference " + EPP_FQN + " — got: " + value);
    }

    @Test
    void boot3DiscoveryViaImportsFileIsRegistered() throws IOException {
        URL resource = currentClassLoader().getResource(
                "META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports");
        assertNotNull(resource, "Boot-3-style .imports file must ship in the JAR");

        String contents;
        try (InputStream in = resource.openStream()) {
            contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(contents.contains(EPP_FQN),
                ".imports file must reference " + EPP_FQN + " — got: " + contents);
    }

    private static ClassLoader currentClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl != null ? cl : DbUrlEnvironmentPostProcessorDiscoveryTest.class.getClassLoader();
    }
}
