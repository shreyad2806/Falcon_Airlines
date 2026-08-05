# Falcon Airlines Backend

Spring Boot 3.x backend foundation for the Falcon Airlines enterprise platform.

## Run

```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

## Profiles

- `dev` — local Docker PostgreSQL
- `prod` — environment-variable driven
- `test` — Testcontainers PostgreSQL

## Verify

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /swagger-ui.html`

## Tests

```bash
mvn test
```
