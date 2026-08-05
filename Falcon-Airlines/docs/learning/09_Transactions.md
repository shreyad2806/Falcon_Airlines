# Transactions and the Persistence Context

## What we implemented

- `BaseIntegrationTest` with Testcontainers to run repository tests inside a Spring-managed transaction.
- Lazy associations that are loaded inside the same transaction to avoid `LazyInitializationException`.

## Why

A `@Transactional` test boundary lets the persistence context stay open, which is required for lazy loading and for rolling back test changes.

## Important annotations

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class BaseIntegrationTest { }
```

```java
@Transactional
@Test
void shouldFindFlightWithAirport() {
    Flight flight = flightRepository.findById(1L).orElseThrow();
    assertThat(flight.getOriginAirport()).isNotNull();
}
```

## Common interview questions

1. **What happens if you access a lazy collection outside a transaction?**  
   `LazyInitializationException` because the Session is closed.

2. **What is the default propagation of `@Transactional`?**  
   `REQUIRED` — join an existing transaction or create a new one.

3. **Does `JpaRepository.save()` commit immediately?**  
   No — it only stores the entity in the persistence context; the transaction commit flushes to the database.

## Best practices

- Keep read-only queries inside a read-only transaction (`@Transactional(readOnly = true)`).
- Flush complex writes only after business validation.
- Use `@Transactional` on service methods, not controllers.
