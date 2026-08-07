package com.falcon.airlines.common;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests. Loads the full Spring context and points it
 * at the Docker Compose-managed PostgreSQL instance on a dedicated test database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    void prepareAuthTestData() {
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
        String port = System.getenv().getOrDefault("POSTGRES_HOST_PORT", "5433");
        String username = System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", "postgres");
        String password = System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD", "postgres");

        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:" + port + "/falcon_airlines_test");
        registry.add("spring.datasource.username", () -> username);
        registry.add("spring.datasource.password", () -> password);
    }
}
