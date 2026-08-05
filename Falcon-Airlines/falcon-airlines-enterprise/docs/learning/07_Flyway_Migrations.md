# Flyway Migrations

## What we implemented

- `V1__baseline.sql` — marks the start of schema history.
- `V2__create_schema.sql` — creates all 17 tables, indexes, constraints and FKs from `DATABASE_DESIGN.md`.
- `V3__seed_reference_data.sql` — inserts roles, admin user, permissions, airports, aircraft and demo flights.

## Why

Versioned SQL migrations give an auditable, repeatable path from an empty database to the current state. This is essential for CI/CD, multiple environments, and rollbacks.

## Important configuration

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

## Code snippet from this project

```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    -- ...
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_roles_active
    ON user_roles (user_id, role_id)
    WHERE is_deleted = FALSE;
```

## Common interview questions

1. **Why `baseline-on-migrate: true`?**  
   It lets Flyway create the schema-history table at the current version without replaying existing migrations.

2. **What is a partial unique index?**  
   A unique index that only applies to a subset of rows, e.g. active `user_roles` assignments.

3. **Can you rename a Flyway file once applied?**  
   No — the checksum is stored in `flyway_schema_history`; renaming breaks repeatability.

## Best practices

- Never modify an already-applied migration.
- One logical change per file.
- Keep seed/reference data in separate migrations from schema changes.
