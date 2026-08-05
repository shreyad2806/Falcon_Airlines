# Indexes and Performance

## What we implemented

- B-tree primary and foreign-key indexes.
- Unique indexes on natural identifiers (`email`, `username`, `iata_code`, `booking_reference`, `ticket_number`).
- Composite indexes for common search patterns (`flights(origin, destination, scheduled_departure)`).
- Partial indexes for soft-delete and status filters.

## Why

Indexes keep reads fast while the foreign-key and `CHECK` constraints protect data quality.

## Code snippet from this project

```sql
CREATE INDEX IF NOT EXISTS idx_flights_search
    ON flights (origin_airport_id, destination_airport_id, scheduled_departure);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_roles_active
    ON user_roles (user_id, role_id)
    WHERE is_deleted = FALSE;
```

## Common interview questions

1. **When is a composite index better than multiple single-column indexes?**  
   When queries filter on the leading columns in order (e.g. origin, destination, then departure).

2. **What is a partial index?**  
   An index over rows that match a `WHERE` clause. It is smaller and more selective.

3. **Do indexes slow down writes?**  
   Yes — every insert/update on an indexed column requires index maintenance, so index only what is queried.

## Best practices

- Index foreign keys, unique natural keys, and frequently filtered columns.
- Covering indexes can avoid table lookups but increase write cost.
- Review slow-query logs in Phase 4 before adding more indexes.
