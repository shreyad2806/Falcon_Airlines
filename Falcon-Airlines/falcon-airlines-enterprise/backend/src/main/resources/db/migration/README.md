# Flyway Migrations

This folder contains versioned SQL migrations for PostgreSQL.

- Migrations follow `V{version}__{description}.sql` naming.
- `V1__init_schema.sql` is intentionally a placeholder for Phase 1.
- Real table creation is planned for Phase 2 based on the approved database design.

Do not modify already-applied migrations. Add new files for changes.
