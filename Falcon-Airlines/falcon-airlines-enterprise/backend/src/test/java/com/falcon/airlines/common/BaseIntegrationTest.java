package com.falcon.airlines.common;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for integration tests. Loads the full Spring context and starts an
 * isolated Testcontainers PostgreSQL instance for the test database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("falcon_airlines_test")
            .withUsername("test")
            .withPassword("test");

    private static final AtomicBoolean SHUTDOWN_HOOK_ADDED = new AtomicBoolean(false);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @BeforeAll
    void prepareAuthTestData() {
        // Reset the Testcontainers database to a deterministic seed state for every
        // integration test class. This is the isolation boundary: one shared
        // PostgreSQL container with a fresh Flyway-migrated schema before each class.
        flyway.clean();
        flyway.migrate();

        String authUserPatterns = "'cust_%' OR username LIKE 'dupuser_%' OR username LIKE 'u1_%' OR username LIKE 'u2_%' OR username LIKE 'login_%' OR username LIKE 'badlogin_%' OR username LIKE 'reset_%' OR username LIKE 'verify_%'";

        jdbcTemplate.update("DELETE FROM email_verification_tokens WHERE user_id IN (SELECT id FROM users WHERE username LIKE " + authUserPatterns + ")");
        jdbcTemplate.update("DELETE FROM password_reset_tokens WHERE user_id IN (SELECT id FROM users WHERE username LIKE " + authUserPatterns + ")");
        jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE username LIKE " + authUserPatterns + ")");
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username LIKE " + authUserPatterns + ")");
        jdbcTemplate.update("DELETE FROM users WHERE username LIKE " + authUserPatterns);
        jdbcTemplate.update("""
                INSERT INTO roles (name, description, is_system, is_deleted, created_at, updated_at)
                SELECT 'CUSTOMER', 'Default customer/passenger role', false, false, NOW(), NOW()
                WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'CUSTOMER')
                """);
    }

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        if (SHUTDOWN_HOOK_ADDED.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (POSTGRES.isRunning()) {
                    POSTGRES.stop();
                }
            }));
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
