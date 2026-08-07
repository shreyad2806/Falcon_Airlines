package com.falcon.airlines.common;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests. Loads the full Spring context and points it
 * at the Docker Compose-managed PostgreSQL instance on a dedicated test database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

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
