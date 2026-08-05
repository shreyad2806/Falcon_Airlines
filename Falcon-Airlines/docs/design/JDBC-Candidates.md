# JDBC Candidates for Phase 5

These areas should be implemented with `JdbcTemplate` or a native query in Phase 5 to avoid JPA overhead.

1. **Flight search with filters**
   - Origin, destination, date range, status, availability.
   - JPA projections are awkward; JDBC/SQL with pagination is faster and clearer.

2. **Seat inventory and allocation**
   - High-concurrency updates on a small number of rows (e.g., `flight_id` + `cabin`).
   - Optimistic locking or `SELECT ... FOR UPDATE` in JDBC is easier to control.

3. **Bulk operations**
   - Mass schedule imports, seasonal price uploads, fleet updates.
   - `JdbcTemplate.batchUpdate` is much more efficient than saving N entities.

4. **Reports and dashboards**
   - Revenue by route, load factor, cancellation rate.
   - Aggregations and window functions in SQL outperform loading entities.

5. **Audit log streaming**
   - High-volume append-only inserts.
   - JDBC avoids the full Hibernate persistence context for each event.

6. **Delay prediction reads/writes**
   - Model output is JSONB and read as raw features.
   - A simple `INSERT`/`SELECT` with JDBC is sufficient.

7. **Ticket/PNR look-ups by natural key**
   - `ticket_number` and `booking_reference` are unique and searched directly.
   - JDBC `Optional<T>` projection avoids loading the full graph.

## Decision rule

Use JPA for domain object lifecycle and small transactional writes. Use JDBC when the operation is set-based, aggregation-heavy, bulk, or needs tight SQL control.
