# Airport and Flight Management - Interview Notes

This document provides interview-ready explanations for key concepts related to airport and flight management in the Falcon Airlines system.

---

## 1. How is the Airport domain designed?

### General Concept
The Airport domain follows a standard layered architecture with entity-repository-service-controller separation. It uses JPA for persistence, Spring Data for data access, and REST for API exposure. The design emphasizes data integrity through validation, uniqueness constraints, and referential integrity checks.

### Falcon Airlines Implementation
- **Entity**: `Airport` class extends `AuditEntity` for audit trail support
- **Repository**: `AirportRepository` extends `JpaRepository` and `JpaSpecificationExecutor` for CRUD and dynamic queries
- **Service**: `AirportService` contains business logic including uniqueness validation and soft delete with referential integrity checks
- **Controller**: `AirportController` exposes REST endpoints at `/api/airports` with method-level security
- **DTO**: `AirportRequest` for input validation, `AirportResponse` for output

Key design decisions:
- Soft delete via `@SQLRestriction("is_deleted = false")` on the entity
- Uniqueness enforced at both database (UNIQUE constraints) and application levels
- Search implemented using JPA Specifications for flexible filtering
- Pagination through Spring Data's `Pageable` interface

---

## 2. Why is airport code unique?

### General Concept
Airport codes (IATA and ICAO) are globally unique identifiers assigned by international aviation organizations. IATA codes are 3-letter codes (e.g., JFK, LHR) while ICAO codes are 4-letter codes (e.g., KJFK, EGLL). Uniqueness is essential to avoid confusion in flight scheduling, ticketing, and operations.

### Falcon Airlines Implementation
- **Database level**: UNIQUE constraints on `iata_code` and `icao_code` columns in the airports table
- **Application level**: `AirportService.validateUniqueness()` checks for duplicates before create/update operations
- **Repository methods**: 
  - `findByIataCode()` and `findByIcaoCode()` for create operations
  - `findByIataCodeAndIdNot()` and `findByIcaoCodeAndIdNot()` for update operations (excludes current record)
- **Error handling**: Throws `BaseException` with code `DUPLICATE_IATA_CODE` or `DUPLICATE_ICAO_CODE` and HTTP status `409 CONFLICT`

This dual enforcement ensures data integrity even if the database constraint is bypassed.

---

## 3. How are Airport and Flight related?

### General Concept
In airline systems, flights connect airports as origin and destination points. This is a many-to-one relationship: many flights can originate from or arrive at a single airport. The relationship is typically modeled with foreign keys in the flight table pointing to the airport table.

### Falcon Airlines Implementation
- **JPA annotations** in `Flight` entity:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "origin_airport_id", nullable = false)
private Airport originAirport;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "destination_airport_id", nullable = false)
private Airport destinationAirport;
```
- **Database schema**: Foreign keys `origin_airport_id` and `destination_airport_id` in flights table reference `airports(id)`
- **Bidirectional**: Not implemented - Airport does not have a collection of flights
- **Fetch strategy**: LAZY to avoid loading airports when not needed
- **Nullable**: false - both airports are required for a flight
- **Validation**: `FlightService.validateFlightSchedule()` ensures origin and destination are different airports

---

## 4. How are Aircraft and Flight related?

### General Concept
Aircraft are physical assets that operate flights over time. A single aircraft can operate many flights sequentially, but at any given time, an aircraft can only be assigned to one flight. This is a many-to-one relationship from flight to aircraft.

### Falcon Airlines Implementation
- **JPA annotation** in `Flight` entity:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "aircraft_id", nullable = false)
private Aircraft aircraft;
```
- **Database schema**: Foreign key `aircraft_id` in flights table references `aircraft(id)`
- **Bidirectional**: Not implemented - Aircraft does not have a collection of flights
- **Fetch strategy**: LAZY
- **Nullable**: false - aircraft is required for a flight
- **Schedule conflict detection**: `FlightService.checkAircraftOverlap()` prevents assigning an aircraft to overlapping flights

---

## 5. Why use @ManyToOne?

### General Concept
`@ManyToOne` is the most common relationship type in JPA, representing a many-to-one association between entities. It's appropriate when many instances of one entity can be associated with a single instance of another entity. The "many" side is typically the owning side with the foreign key.

### Falcon Airlines Implementation
- **Flight → Airport**: Many flights can use one airport (as origin or destination)
- **Flight → Aircraft**: Many flights can use one aircraft over time
- **Owning side**: Flight is the owning side in all relationships (contains `@JoinColumn`)
- **Foreign key**: Stored in the flights table
- **Benefits**: 
  - Natural mapping to relational database schema
  - Efficient for queries that start from flight
  - Supports lazy loading of related entities
  - No need for join tables (unlike @ManyToMany)

---

## 6. What is the owning side of a JPA relationship?

### General Concept
In JPA bidirectional relationships, one side is designated as the "owning side" - the side that determines how the relationship is mapped to the database. The owning side contains the `@JoinColumn` annotation and is responsible for persisting the foreign key. The other side is the "inverse" or "mapped-by" side.

### Falcon Airlines Implementation
- **Current design**: All relationships are unidirectional from Flight to Airport/Aircraft
- **Owning side**: Flight is the owning side (contains `@JoinColumn`)
- **No inverse side**: Airport and Aircraft do not have collections of Flight entities
- **Simplification**: This design avoids bidirectional synchronization complexity
- **Query approach**: Flight queries are executed through `FlightRepository` with specifications

If bidirectional relationships were added, the inverse side would use `@OneToMany(mappedBy = "...")` and would not have `@JoinColumn`.

---

## 7. Why use DTOs?

### General Concept
Data Transfer Objects (DTOs) are objects used to transfer data between layers of an application, particularly between the service layer and the presentation layer. They provide separation between the internal domain model and the external API contract, enabling control over what data is exposed and accepted.

### Falcon Airlines Implementation
- **Request DTOs**: `AirportRequest`, `AircraftRequest`, `FlightRequest` for input validation
  - Contain validation annotations (`@NotBlank`, `@Size`, `@NotNull`, `@Min`)
  - Separate from entity to avoid exposing internal fields
  - Enable selective field updates (e.g., not all entity fields are updatable via API)
- **Response DTOs**: `AirportResponse`, `AircraftResponse`, `FlightResponse` for output
  - Contain only fields needed by API consumers
  - Can be shaped differently from entities (e.g., nested objects, computed fields)
  - Mapped using MapStruct mappers (`AirportMapper`, `AircraftMapper`, `FlightMapper`)
- **Benefits**:
  - Security: Don't expose sensitive entity fields
  - Validation: Centralized validation rules on request DTOs
  - Flexibility: API contract independent of domain model
  - Performance: Can include only needed data in responses

---

## 8. How does pagination work?

### General Concept
Pagination is the process of dividing large result sets into smaller, manageable chunks (pages). It improves performance by reducing memory usage and network traffic, and improves user experience by presenting data in digestible portions. Spring Data provides built-in pagination support through the `Pageable` interface.

### Falcon Airlines Implementation
- **Controller level**: Accepts `Pageable` parameter in controller methods
```java
@GetMapping
public ResponseEntity<ApiResponse<Page<AirportResponse>>> searchAirports(..., Pageable pageable)
```
- **Service level**: Passes `Pageable` to repository
```java
public Page<AirportResponse> searchAirports(..., Pageable pageable) {
    return airportRepository.findAll(spec, pageable).map(airportMapper::toResponse);
}
```
- **Repository level**: Uses `JpaSpecificationExecutor.findAll(spec, pageable)`
- **Client parameters**: 
  - `page` - zero-based page number
  - `size` - number of items per page
  - `sort` - sorting criteria (field,direction)
- **Response**: `Page<T>` contains:
  - Content (list of items)
  - Total elements
  - Total pages
  - Current page number
  - Page size
  - Whether there are next/previous pages

---

## 9. How does sorting work?

### General Concept
Sorting allows clients to order query results by specific fields in ascending or descending order. Spring Data's `Pageable` interface includes sorting capabilities, enabling flexible, client-controlled sorting without custom query logic.

### Falcon Airlines Implementation
- **Client-controlled**: Sorting is specified via query parameters
  - Example: `GET /api/flights?sort=scheduledDeparture,desc`
  - Multiple sorts: `GET /api/flights?sort=status,asc&sort=flightNumber,desc`
- **Spring Data support**: `Pageable.getSort()` contains sort definitions
- **Repository handling**: Spring Data automatically generates ORDER BY clauses
- **Default sort**: No default sort specified - relies on client input
- **Sortable fields**: Any entity field can be sorted (flightNumber, scheduledDeparture, status, etc.)
- **JPA Specification**: When using specifications, sorting is applied separately via Pageable

---

## 10. How does flight search work?

### General Concept
Flight search allows filtering flights by various criteria such as origin, destination, date, status, and flight number. Dynamic query building enables flexible search without writing separate queries for each combination. JPA Specifications provide a type-safe way to construct dynamic queries programmatically.

### Falcon Airlines Implementation
- **Search parameters** in `FlightController.searchFlights()`:
  - `flightNumber` - partial match (case-insensitive)
  - `originAirport` - exact match on IATA code
  - `destinationAirport` - exact match on IATA code
  - `aircraft` - exact match on registration number
  - `status` - exact match on FlightStatus enum
  - `departureFrom` - range filter (>=)
  - `departureTo` - range filter (<=)
  - `active` - exact match on boolean
- **Specification building** in `FlightService.buildSpecification()`:
```java
Specification<Flight> spec = (root, query, cb) -> cb.equal(root.get("isDeleted"), false);

if (flightNumber != null && !flightNumber.isBlank()) {
    String like = "%" + flightNumber.toLowerCase() + "%";
    spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("flightNumber")), like));
}

if (originAirport != null && !originAirport.isBlank()) {
    spec = spec.and((root, query, cb) -> cb.equal(root.get("originAirport").get("iataCode"), originAirport));
}
```
- **Repository query**: `flightRepository.findAll(spec, pageable)`
- **Relationship navigation**: Airport and aircraft filters navigate relationships using `root.get("originAirport").get("iataCode")`

---

## 11. How is flight schedule validation implemented?

### General Concept
Flight schedule validation ensures that flight times are logically consistent. Key validations include: origin and destination must be different airports, departure time must be before arrival time, and departure and arrival cannot be at the same time. These validations prevent nonsensical or impossible flight schedules.

### Falcon Airlines Implementation
- **Validation method**: `FlightService.validateFlightSchedule(FlightRequest request)`
- **Airport validation**:
```java
if (request.getOriginAirportId().equals(request.getDestinationAirportId())) {
    throw new BaseException("Departure and arrival airports must be different", 
        HttpStatus.BAD_REQUEST, "FLIGHT_SAME_AIRPORT");
}
```
- **Time validation**:
```java
if (request.getScheduledDeparture().equals(request.getScheduledArrival())) {
    throw new BaseException("Departure and arrival times cannot be the same", 
        HttpStatus.BAD_REQUEST, "FLIGHT_SAME_TIME");
}

if (request.getScheduledDeparture().isAfter(request.getScheduledArrival())) {
    throw new BaseException("Departure time must be before arrival time", 
        HttpStatus.BAD_REQUEST, "FLIGHT_INVALID_SCHEDULE");
}
```
- **Entity existence validation**: `resolveAirport()` and `resolveAircraft()` throw `404 NOT_FOUND` if referenced entities don't exist
- **Called during**: Both create and update operations before persistence

---

## 12. How are duplicate flights prevented?

### General Concept
Duplicate flight detection prevents scheduling the same flight (same flight number) at the same time. This could occur due to concurrent requests or user error. The definition of "duplicate" varies by system but typically includes flight number and departure time.

### Falcon Airlines Implementation
- **Definition of duplicate**: Same flight number AND same scheduled departure time
- **Detection method**: `FlightService.checkDuplicateFlight()`
- **Repository queries**:
  - For create: `findByFlightNumberAndScheduledDeparture(flightNumber, scheduledDeparture)`
  - For update: `findByFlightNumberAndScheduledDepartureAndIdNot(..., excludeId)` - excludes current record
- **Implementation**:
```java
private void checkDuplicateFlight(String flightNumber, Instant scheduledDeparture, Long excludeId) {
    Optional<Flight> duplicate;
    if (excludeId == null) {
        duplicate = flightRepository.findByFlightNumberAndScheduledDeparture(flightNumber, scheduledDeparture);
    } else {
        duplicate = flightRepository.findByFlightNumberAndScheduledDepartureAndIdNot(
            flightNumber, scheduledDeparture, excludeId);
    }

    duplicate.ifPresent(f -> {
        throw new BaseException("Duplicate flight: a flight with number " + flightNumber + 
            " and the same departure time already exists",
            HttpStatus.CONFLICT, "FLIGHT_DUPLICATE");
    });
}
```
- **Error response**: `409 CONFLICT` with error code `FLIGHT_DUPLICATE`

---

## 13. Where does business logic live?

### General Concept
In layered architecture, business logic typically resides in the service layer. This separation keeps controllers thin (focused on HTTP concerns) and entities focused on data persistence. The service layer orchestrates operations, applies business rules, and coordinates with repositories and other services.

### Falcon Airlines Implementation
- **Service layer**: All business logic is in `AirportService`, `AircraftService`, and `FlightService`
  - Validation: `validateUniqueness()`, `validateFlightSchedule()`
  - Business rules: `checkDuplicateFlight()`, `checkAircraftOverlap()`
  - Entity resolution: `resolveAirport()`, `resolveAircraft()`
  - Soft delete logic with referential integrity checks
- **Controller layer**: Thin controllers that delegate to services
  - Handle HTTP concerns: request mapping, parameter binding, response formatting
  - Security: `@PreAuthorize` annotations
  - No business logic beyond delegation
- **Entity layer**: Focused on data persistence with JPA annotations
- **Repository layer**: Data access only, no business logic

This separation enables:
- Testability: Services can be unit tested without HTTP layer
- Reusability: Business logic can be reused by multiple controllers
- Maintainability: Clear separation of concerns

---

## 14. Why should controllers remain thin?

### General Concept
Thin controllers follow the Single Responsibility Principle - controllers should only handle HTTP-specific concerns (request/response mapping, validation triggering, security). Business logic belongs in the service layer. This separation improves testability, reusability, and maintainability.

### Falcon Airlines Implementation
- **Controller responsibilities**:
  - Map HTTP methods to service operations
  - Extract request parameters and path variables
  - Trigger validation via `@Valid`
  - Apply security via `@PreAuthorize`
  - Format responses using `ApiResponse<T>`
  - Handle HTTP status codes
- **Service responsibilities**:
  - All business logic and validation
  - Entity lifecycle management
  - Transaction boundaries
  - Exception throwing
- **Example** from `AirportController`:
```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<AirportResponse>> createAirport(@Valid @RequestBody AirportRequest request) {
    AirportResponse response = airportService.createAirport(request);  // Delegation only
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Airport created successfully", response));
}
```

Benefits:
- Controllers can be tested with mock services
- Business logic can be reused by other controllers or scheduled jobs
- Easier to understand and maintain each layer

---

## 15. How does referential integrity work?

### General Concept
Referential integrity ensures that relationships between tables remain consistent. It prevents orphaned records and ensures that foreign key values reference valid primary keys. Databases enforce this through foreign key constraints. Application-level checks provide better error messages and additional business rules.

### Falcon Airlines Implementation
- **Database level**: Foreign key constraints in schema
```sql
origin_airport_id BIGINT NOT NULL REFERENCES airports(id)
destination_airport_id BIGINT NOT NULL REFERENCES airports(id)
aircraft_id BIGINT NOT NULL REFERENCES aircraft(id)
```
- **Application level**: Service layer checks before deletion
```java
// AirportService.deleteAirport()
if (flightRepository.existsByOriginAirportIdAndIsActiveTrue(id) ||
    flightRepository.existsByDestinationAirportIdAndIsActiveTrue(id)) {
    throw new BaseException("Airport is referenced by active flights", 
        HttpStatus.CONFLICT, "AIRPORT_IN_USE");
}

// AircraftService.deleteAircraft()
if (flightRepository.existsByAircraftIdAndIsActiveTrue(id)) {
    throw new BaseException("Aircraft is referenced by active flights", 
        HttpStatus.CONFLICT, "AIRCRAFT_IN_USE");
}
```
- **Benefits of application checks**:
  - Better error messages for API consumers
  - Check only active references (ignoring soft-deleted flights)
  - Business rule enforcement (e.g., prevent deletion of airports with active flights)
- **Database constraint**: Final safety net if application checks are bypassed

---

## 16. What happens if an airport is referenced by flights?

### General Concept
When an entity is referenced by other entities, deleting it would violate referential integrity. Systems typically handle this by either: (1) preventing deletion (restrict), (2) cascading the delete to dependent records (cascade), or (3) soft deleting and keeping references (soft delete with checks).

### Falcon Airlines Implementation
- **Soft delete approach**: Airports are soft-deleted (set `is_deleted = true`, `deleted_at = now()`)
- **Prevention check**: Before deletion, service checks for active references
```java
if (flightRepository.existsByOriginAirportIdAndIsActiveTrue(id) ||
    flightRepository.existsByDestinationAirportIdAndIsActiveTrue(id)) {
    throw new BaseException("Airport is referenced by active flights", 
        HttpStatus.CONFLICT, "AIRPORT_IN_USE");
}
```
- **Only active flights checked**: Ignores flights that are already soft-deleted (`is_active = false` or `is_deleted = true`)
- **Error response**: `409 CONFLICT` with clear error message
- **Result**: If no active references, airport is soft-deleted. Existing flights continue to reference the airport (which is fine since it's just soft-deleted and can be reactivated)

---

## 17. What indexes support flight search?

### General Concept
Database indexes improve query performance by allowing the database to quickly locate rows without scanning the entire table. Composite indexes support queries that filter on multiple columns. Partial indexes reduce index size by indexing only a subset of rows.

### Falcon Airlines Implementation
- **Primary search index**:
```sql
CREATE INDEX idx_flights_search ON flights (origin_airport_id, destination_airport_id, scheduled_departure);
```
  Supports the most common search pattern: find flights between two airports around a specific time
- **Aircraft lookup index**:
```sql
CREATE INDEX idx_flights_aircraft ON flights (aircraft_id);
```
  Supports queries for flights by aircraft (used in overlap detection)
- **Status filter index**:
```sql
CREATE INDEX idx_flights_status ON flights (status);
```
  Supports filtering by flight status
- **Active flights partial index**:
```sql
CREATE INDEX idx_flights_active ON flights (is_active) WHERE is_active = TRUE AND is_deleted = FALSE;
```
  Efficiently filters for active flights only, reducing index size
- **Airport indexes**:
```sql
CREATE INDEX idx_airports_country_active ON airports (country, is_active);
```
  Supports searching airports by country and active status

These indexes are defined in the Flyway migration `V2__create_schema.sql`.

---

## 18. What is the N+1 problem?

### General Concept
The N+1 query problem occurs when loading N parent entities and then executing N additional queries to load their related entities, resulting in N+1 total queries instead of 1. This happens with lazy loading when accessing relationships in a loop. It causes significant performance degradation.

### Falcon Airlines Implementation
- **Current state**: All `@ManyToOne` relationships use `FetchType.LAZY`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "origin_airport_id", nullable = false)
private Airport originAirport;
```
- **Potential N+1 scenario**:
```java
List<Flight> flights = flightRepository.findAll();  // 1 query
flights.forEach(f -> System.out.println(f.getOriginAirport().getName()));  // N queries
```
- **Not currently mitigated**: The implementation does not include JOIN FETCH or EntityGraph
- **Mitigation strategies** (if needed):
  1. JOIN FETCH in repository queries:
  ```java
  @Query("SELECT f FROM Flight f JOIN FETCH f.originAirport JOIN FETCH f.destinationAirport")
  List<Flight> findAllWithAirports();
  ```
  2. EntityGraph:
  ```java
  @EntityGraph(attributePaths = {"originAirport", "destinationAirport"})
  List<Flight> findAll();
  ```
  3. DTO projections to fetch only needed fields without loading full entities

The current design prioritizes simplicity over optimization, which is acceptable for the current scale. Performance monitoring would indicate if N+1 becomes an issue.

---

## 19. How would flight inventory scale?

### General Concept
Scaling flight inventory involves handling increasing data volume, query load, and concurrent operations. Strategies include: database indexing, caching, read replicas, sharding, and architectural patterns like CQRS. The choice depends on specific bottlenecks and growth patterns.

### Falcon Airlines Implementation - Current State
- **Single database**: PostgreSQL with appropriate indexes
- **JPA Specifications**: Flexible but may not optimize for all query patterns
- **No caching**: All queries hit the database
- **No read replicas**: Single database instance
- **Soft delete**: Keeps historical data (may grow unbounded)

### Scaling Considerations
1. **Database optimization**:
   - Current indexes support common queries
   - Could add composite indexes for specific query patterns
   - Partitioning by date for large flight tables

2. **Caching**:
   - Cache frequently accessed airports and aircraft (rarely change)
   - Cache flight search results for common routes/dates
   - Use Redis or in-memory cache

3. **Read replicas**:
   - Route read queries to replicas
   - Writes go to primary
   - Reduces load on primary database

4. **Architectural patterns**:
   - CQRS: Separate read and write models
   - Event sourcing: Rebuild read models from events
   - Materialized views: Pre-computed search results

5. **Search optimization**:
   - Elasticsearch for full-text search
   - Specialized search service separate from transactional database

6. **Data retention**:
   - Archive old flights to separate storage
   - Implement data retention policies

The current implementation is suitable for early-stage development. Scaling decisions should be data-driven based on actual performance metrics.

---

## 20. Explain Falcon Airlines flight management in 60 seconds.

Falcon Airlines flight management is a Spring Boot application with a three-layer architecture: controllers handle REST endpoints at `/api/flights`, services contain business logic, and repositories use Spring Data JPA for database access. The Flight entity has many-to-one relationships to Airport (origin and destination) and Aircraft, all using lazy loading. Flights are validated for schedule consistency, duplicate prevention, and aircraft schedule conflicts before persistence. Search uses JPA Specifications for dynamic filtering by flight number, airports, aircraft, status, and date range. Pagination and sorting are handled through Spring Data's Pageable interface. Security is enforced with JWT authentication and method-level `@PreAuthorize` annotations requiring FLIGHT_READ or FLIGHT_WRITE authorities. Soft delete is implemented via `@SQLRestriction("is_deleted = false")` on entities. The system uses PostgreSQL with indexes supporting common query patterns, and referential integrity is enforced through both database foreign key constraints and application-level checks before deletion.
