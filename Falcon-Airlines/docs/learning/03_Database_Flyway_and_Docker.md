# Database, Flyway and Docker

## What we implemented

- PostgreSQL 16 running via Docker Compose
- Flyway migration `V1__baseline.sql`
- Multi-stage `Dockerfile` with health check
- `.env.example` for environment variables

## Why

- Flyway gives versioned, repeatable and auditable schema changes.
- Docker Compose lets the local stack mirror the production runtime.
- The health check proves the container is actually serving traffic, not just running the JVM.

## Important configuration

`application-dev.yml` Flyway section:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

`docker-compose.yml` health check on PostgreSQL:

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
  interval: 10s
  timeout: 5s
  retries: 5
```

`Dockerfile` health check:

```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
```

## Common interview questions

1. **What does `baseline-on-migrate: true` do?**  
   It creates the `flyway_schema_history` table at the current version without executing existing migrations, allowing Flyway to take over a pre-existing database.

2. **Why `spring.jpa.hibernate.ddl-auto: none`?**  
   Prevents Hibernate from creating or modifying tables. Flyway owns the schema.

3. **What is `depends_on: condition: service_healthy` in Docker Compose?**  
   It starts the backend only after PostgreSQL reports itself as healthy.

## Best practices

- One logical change per migration file.
- Name migrations `V{version}__{description}.sql`.
- Use `baselineOnMigrate` when introducing Flyway to an existing database.
- Keep the Dockerfile as a multi-stage build for a smaller, non-root runtime image.
