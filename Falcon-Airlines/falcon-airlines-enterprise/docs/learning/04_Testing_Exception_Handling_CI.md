# Testing, Exception Handling and CI

## What we implemented

- JUnit 5 and Mockito via `BaseUnitTest`
- Testcontainers PostgreSQL via `BaseIntegrationTest`
- `@RestControllerAdvice` global exception handler (`GlobalExceptionHandler`)
- GitHub Actions build and test workflows

## Why

- Unit tests isolate business logic.
- Integration tests verify real database and Spring context behaviour.
- A global exception handler returns a uniform `ApiErrorResponse` envelope.
- CI catches regressions before code is merged.

## Important code snippets

Integration test base:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class BaseIntegrationTest { }
```

Testcontainers configuration:

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("falcon_test")
                .withUsername("test")
                .withPassword("test");
    }
}
```

Exception handler:

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler { }
```

GitHub Actions build step:

```yaml
- name: Build with Maven
  working-directory: Falcon-Airlines/falcon-airlines-enterprise/backend
  run: mvn -B -DskipTests clean package
```

## Common interview questions

1. **Why `@RestControllerAdvice`?**  
   It centralises exception handling for all REST controllers, producing a consistent error response format.

2. **What does `BaseUnitTest` with `@ExtendWith(MockitoExtension.class)` do?**  
   Enables Mockito lifecycle, strict stubbing and JUnit 5 integration for unit tests.

3. **How does Testcontainers work here?**  
   `@ServiceConnection` on a `PostgreSQLContainer` bean tells Spring Boot to use that container as the test `DataSource`.

## Best practices

- Run integration tests on a `RANDOM_PORT` to avoid port conflicts.
- Include trace IDs and request paths in error responses for production debugging.
- Keep CI workflows fast: `build` packages, `test` verifies.
