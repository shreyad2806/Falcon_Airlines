# JPA Relationships

## What we implemented

- `@ManyToOne` on the child side for every foreign key (e.g., `Flight` -> `Airport`, `Ticket` -> `Flight`).
- `@OneToMany` on the parent side where the relationship is naturally navigated (e.g., `User` -> `Bookings`).
- Join entities for many-to-many and association tables (`UserRole`, `RolePermission`, `BookingPassenger`).
- `FetchType.LAZY` by default to avoid accidental N+1 queries.

## Why

The ER diagram in `DATABASE_DESIGN.md` is modelled as a relational schema. JPA relationships map that schema directly to objects so that Hibernate can load and persist related records without manual SQL joins.

## Important annotations

- `@ManyToOne(fetch = FetchType.LAZY)` — the owning side of the relationship.
- `@OneToMany(mappedBy = "...", fetch = FetchType.LAZY)` — the inverse side.
- `@JoinColumn(name = "...")` — the physical FK column name.
- `@SQLRestriction("is_deleted = false")` — hides soft-deleted rows at the ORM level.

## Code snippet from this project

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "origin_airport_id", nullable = false)
private Airport originAirport;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "destination_airport_id", nullable = false)
private Airport destinationAirport;
```

## Common interview questions

1. **What is the difference between `@ManyToOne` and `@OneToMany`?**  
   `@ManyToOne` is placed on the child/owner side and stores the FK. `@OneToMany` is the inverse side and uses `mappedBy`.

2. **Why use `FetchType.LAZY`?**  
   It loads the related entity only when accessed, preventing N+1 and out-of-memory issues.

3. **How do you map a many-to-many with extra columns?**  
   Create a join entity with two `@ManyToOne` mappings and any additional fields.

## Best practices

- Avoid `CascadeType.REMOVE` on non-orphan, non-owning sides.
- Keep associations lazy by default; explicitly fetch when needed.
- Use join entities when the link table has its own attributes (`valid_from`, `fare_class`, etc.).
