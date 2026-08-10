# Passenger Management - Interview Notes

This document provides interview-ready explanations for key concepts related to passenger management in the Falcon Airlines system.

---

## 1. Why use DTOs?

### General Concept
Data Transfer Objects (DTOs) are simple objects used to transfer data between layers of an application. They separate the API layer from the domain layer, providing better control over data serialization, validation, and security.

### Falcon Airlines Implementation
- **PassengerRequest**: Used for creating and updating passengers via POST and PUT endpoints
- **PassengerResponse**: Used for returning passenger data via GET endpoints
- **Location**: Both DTOs are in `com.falcon.airlines.dto.request` and `com.falcon.airlines.dto.response` packages
- **Mapping**: MapStruct's `PassengerMapper` handles conversion between DTOs and entities
- **Validation**: Bean Validation annotations (`@NotBlank`, `@Pattern`, `@Email`, etc.) are applied to `PassengerRequest`

Key benefits:
- **Security**: Prevents over-posting and information leakage
- **Validation**: Enables validation at the DTO level before entity persistence
- **Flexibility**: Allows different representations for different use cases
- **Performance**: Enables selective field loading and optimized payloads
- **Maintainability**: Separates API contract from domain model

---

## 2. Why not expose JPA entities?

### General Concept
Exposing JPA entities directly to clients creates security risks, couples the API to the domain model, and can lead to performance issues like N+1 queries. DTOs provide a clean separation between the API and domain layers.

### Falcon Airlines Implementation
- **Entity**: `Passenger` entity with JPA annotations, audit fields, and relationships
- **DTO**: `PassengerResponse` with only the fields needed for API responses
- **Controller**: `PassengerController` only works with DTOs, never entities
- **Service**: `PassengerService` maps between entities and DTOs using `PassengerMapper`

Specific protections:
- **Audit fields**: `createdAt`, `updatedAt`, `createdBy`, `updatedBy` are not exposed in requests
- **Internal IDs**: Only the passenger `id` is exposed, not internal relationship IDs
- **Computed fields**: `ticketCount` and `bookingCount` are computed in the service layer, not stored in the entity
- **Lazy loading**: Entity relationships are not exposed, preventing accidental N+1 queries

---

## 3. How is passenger validation implemented?

### General Concept
Passenger validation is implemented at two layers: Bean Validation for format and constraint validation on DTOs, and service-layer business validation for complex business rules.

### Falcon Airlines Implementation

**Bean Validation (DTO Layer)**:
- **Location**: `PassengerRequest` class
- **Annotations**:
  - `@NotBlank` on `firstName`, `lastName` (required fields)
  - `@NotNull`, `@Past` on `dateOfBirth` (required, must be past)
  - `@Email`, `@Size(max = 255)` on `email` (optional, valid format)
  - `@Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,20}$")` on `phone` (optional, phone pattern)
  - `@Pattern(regexp = "^[A-Z0-9<]{6,50}$")` on `passportNumber` (optional, alphanumeric)
  - `@Pattern(regexp = "^[A-Z]{3}$")` on `nationality` (optional, 3-letter ISO code)
  - `@NotNull` on `gender` (required, enum)
  - `@Size(max = 20)` on `redressNumber` (optional)
- **Execution**: Automatic when `@Valid` is used in controller methods
- **Error**: Returns 400 BAD_REQUEST with field-specific error messages

**Service-Layer Business Validation**:
- **Location**: `PassengerService` class
- **Methods**:
  - `validateUserExistence()`: Checks if user exists when userId is provided
  - `validateUniqueness()`: Checks passport number and email uniqueness
  - `validatePassengerNotInUse()`: Checks if passenger is referenced by tickets or bookings before deletion
- **Execution**: Explicit calls in service methods
- **Error**: Throws `BaseException` with 404 NOT_FOUND or 409 CONFLICT

This separation ensures simple format validation is handled automatically by the framework, while complex business logic remains in the service layer.

---

## 4. Where should business validation live?

### General Concept
Business validation should live in the service layer, not in controllers or entities. The service layer is the appropriate place for validation that requires database access, spans multiple entities, or implements complex business rules.

### Falcon Airlines Implementation
- **Service Layer**: `PassengerService` contains all business validation logic
- **Validation Methods**:
  - `validateUserExistence(Long userId)`: Queries `UserRepository.existsById()`
  - `validateUniqueness(PassengerRequest request, Long excludeId)`: Queries `PassengerRepository` for duplicates
  - `validatePassengerNotInUse(Long id)`: Queries `TicketRepository` and `BookingPassengerRepository`
- **Controller Layer**: Only handles HTTP concerns (request/response mapping, status codes)
- **Entity Layer**: Contains only JPA annotations and field definitions
- **DTO Layer**: Contains only Bean Validation annotations for format validation

**Rationale**:
- **Testability**: Service layer validation can be unit tested with mocked repositories
- **Reusability**: Validation logic can be reused across multiple endpoints
- **Separation of Concerns**: Controllers handle HTTP, services handle business logic
- **Transaction Management**: Service methods can be transactional, ensuring data consistency

---

## 5. How does passport uniqueness work?

### General Concept
Passport numbers must be unique across all passengers to prevent duplicate passenger records. This is enforced at both database and application levels for defense in depth.

### Falcon Airlines Implementation

**Database Level**:
- **Constraint**: UNIQUE constraint on `passport_number` column in passengers table
- **Location**: Flyway migration `V2__create_schema.sql`
- **Effect**: Database rejects duplicate passport numbers at insert/update time

**Application Level**:
- **Service Method**: `PassengerService.validateUniqueness()`
- **Repository Methods**:
  - `findByPassportNumber(String passportNumber)` - for create operations
  - `findByPassportNumberAndIdNot(String passportNumber, Long id)` - for update operations (excludes current record)
- **Validation Flow**:
  1. On create: Check if passport number already exists in database
  2. On update: Check if passport number exists for any other passenger
  3. If duplicate found: Throw `BaseException` with code `DUPLICATE_PASSPORT` and status `409 CONFLICT`

**Error Handling**:
```java
if (existingByPassport.isPresent()) {
    throw new BaseException(HttpStatus.CONFLICT, "DUPLICATE_PASSPORT", 
            "Passport number already exists");
}
```

This dual enforcement ensures data integrity even if the database constraint is bypassed or if the application-level validation fails.

---

## 6. How does passenger search work?

### General Concept
Passenger search is implemented using JPA Specifications, which allow dynamic query construction based on optional filter parameters. This provides flexible search capabilities without writing custom SQL queries.

### Falcon Airlines Implementation

**Search Parameters**:
- **firstName**: Case-insensitive partial match on first name
- **lastName**: Case-insensitive partial match on last name
- **email**: Case-insensitive partial match on email
- **passportNumber**: Case-insensitive partial match on passport number
- **userId**: Exact match on user ID
- **fullName**: Case-insensitive partial match on both first and last name

**Implementation**:
- **Service Method**: `PassengerService.searchPassengers()`
- **Specification Builder**: `buildSpecification()` method constructs dynamic queries
- **Repository**: `PassengerRepository` extends `JpaSpecificationExecutor<Passenger>`

**Example Specification**:
```java
Specification<Passenger> spec = (root, query, cb) -> cb.equal(root.get("isDeleted"), false);

if (firstName != null && !firstName.isBlank()) {
    String like = "%" + firstName.toLowerCase() + "%";
    spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("firstName")), like));
}

if (fullName != null && !fullName.isBlank()) {
    String like = "%" + fullName.toLowerCase() + "%";
    spec = spec.and((root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("firstName")), like),
            cb.like(cb.lower(root.get("lastName")), like)));
}
```

**Pagination**:
- **Interface**: Spring Data's `Pageable`
- **Parameters**: `page`, `size`, `sort` query parameters
- **Return Type**: `Page<PassengerResponse>`

**Endpoint**:
- **URL**: `GET /api/passengers?firstName=John&lastName=Doe&page=0&size=10&sort=firstName,asc`
- **Authorization**: `@PreAuthorize("hasAnyAuthority('PASSENGER_READ')")`

---

## 7. How does pagination work?

### General Concept
Pagination is implemented using Spring Data's `Pageable` interface, which provides a standard way to handle pagination, sorting, and page navigation without writing custom SQL.

### Falcon Airlines Implementation

**Controller Layer**:
```java
@GetMapping
public ResponseEntity<ApiResponse<Page<PassengerResponse>>> searchPassengers(
        @RequestParam(required = false) String firstName,
        @RequestParam(required = false) String lastName,
        // ... other params
        Pageable pageable) {
    Page<PassengerResponse> result = passengerService.searchPassengers(..., pageable);
    return ResponseEntity.ok(ApiResponse.ok("Passengers retrieved successfully", result));
}
```

**Service Layer**:
```java
public Page<PassengerResponse> searchPassengers(..., Pageable pageable) {
    Specification<Passenger> spec = buildSpecification(...);
    Page<Passenger> entities = passengerRepository.findAll(spec, pageable);
    return entities.map(passengerMapper::toResponse);
}
```

**Repository Layer**:
```java
public interface PassengerRepository extends JpaRepository<Passenger, Long>, 
        JpaSpecificationExecutor<Passenger> {
    // JpaSpecificationExecutor provides findAll(Specification, Pageable)
}
```

**Query Parameters**:
- **page**: Page number (0-indexed)
- **size**: Number of items per page
- **sort**: Sort field and direction (e.g., `sort=firstName,asc`)

**Example URL**:
```
GET /api/passengers?page=0&size=10&sort=firstName,asc
```

**Response Structure**:
```json
{
  "content": [...],
  "pageable": {...},
  "totalPages": 5,
  "totalElements": 50,
  "size": 10,
  "number": 0
}
```

**Benefits**:
- **Standardized**: Uses Spring Data conventions
- **Efficient**: Database-level pagination (LIMIT/OFFSET)
- **Flexible**: Supports sorting and filtering
- **Type-safe**: Strongly typed Pageable interface

---

## 8. How is passenger history implemented?

### General Concept
Passenger history is implemented by counting the number of tickets and bookings associated with a passenger. This provides a quick overview of a passenger's travel activity without exposing sensitive booking details.

### Falcon Airlines Implementation

**Endpoint**:
- **URL**: `GET /api/passengers/{id}/history`
- **Controller**: `PassengerController.getPassengerHistory()`
- **Service**: `PassengerService.getPassengerHistory()`
- **Authorization**: `@PreAuthorize("hasAnyAuthority('PASSENGER_READ')")`

**Implementation**:
```java
public PassengerResponse getPassengerHistory(Long id) {
    Passenger passenger = getPassengerOrThrow(id);
    PassengerResponse response = passengerMapper.toResponse(passenger);
    response.setTicketCount(ticketRepository.findByPassengerId(id).size());
    response.setBookingCount(bookingPassengerRepository.findByPassengerId(id).size());
    return response;
}
```

**Repository Methods**:
- `TicketRepository.findByPassengerId(Long id)`: Returns list of tickets for passenger
- `BookingPassengerRepository.findByPassengerId(Long id)`: Returns list of booking-passenger links

**Response Fields**:
- `ticketCount`: Number of tickets associated with the passenger
- `bookingCount`: Number of bookings associated with the passenger

**Relationships**:
- **Passenger → Ticket**: One-to-many (via `Ticket.passenger` field)
- **Passenger → BookingPassenger**: One-to-many (via `BookingPassenger.passenger` field)
- **BookingPassenger → Booking**: Many-to-one (via `BookingPassenger.booking` field)

**Response Example**:
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "ticketCount": 5,
  "bookingCount": 3,
  ...
}
```

**Note**: This implementation uses actual relationships from the domain model (Ticket and BookingPassenger entities) and does not create fake history data.

---

## 9. How do JPA relationships work?

### General Concept
JPA relationships define how entities are related to each other in the database. Falcon Airlines uses `@ManyToOne`, `@OneToMany`, and `@ManyToMany` annotations to model relationships, with lazy loading to optimize performance.

### Falcon Airlines Implementation

**Passenger Relationships**:

**Passenger → User (Optional)**:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;
```
- **Type**: Many-to-one (many passengers can link to one user)
- **Fetch**: Lazy (loaded only when accessed)
- **Database**: Foreign key `user_id` references users(id)

**Ticket → Passenger (Required)**:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "passenger_id", nullable = false)
private Passenger passenger;
```
- **Type**: Many-to-one (many tickets can belong to one passenger)
- **Fetch**: Lazy (loaded only when accessed)
- **Database**: Foreign key `passenger_id` references passengers(id)
- **Constraint**: nullable = false (required relationship)

**BookingPassenger → Passenger (Required)**:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "passenger_id", nullable = false)
private Passenger passenger;
```
- **Type**: Many-to-one (many booking-passenger links can point to one passenger)
- **Fetch**: Lazy (loaded only when accessed)
- **Database**: Foreign key `passenger_id` references passengers(id)
- **Constraint**: nullable = false (required relationship)

**Key Design Decisions**:
- **Lazy Loading**: Prevents N+1 queries by loading relationships only when needed
- **No Bidirectional Relationships**: Passenger entity does not maintain collections of tickets or booking-passengers (avoids circular dependencies)
- **Referential Integrity**: Database foreign key constraints ensure data integrity
- **Soft Delete**: `@SQLRestriction("is_deleted = false")` filters out deleted entities from queries

---

## 10. How would you scale passenger search?

### General Concept
Scaling passenger search requires optimizing database queries, adding caching, implementing indexing strategies, and potentially using search engines for large datasets.

### Falcon Airlines Implementation (Current State)
- **JPA Specifications**: Dynamic query construction with LIKE predicates
- **Database Indexes**: Unique index on passport_number
- **Pagination**: Limits result set size with Pageable
- **Soft Delete Filter**: `@SQLRestriction` automatically filters deleted records

**Potential Improvements for Scale**:

**Database-Level Optimizations**:
- **Composite Indexes**: Add indexes on (first_name, last_name) for full name searches
- **Partial Indexes**: Create indexes on active passengers only (WHERE is_deleted = false)
- **Full-Text Search**: Use PostgreSQL full-text search for better text matching
- **Query Optimization**: Use EXPLAIN ANALYZE to identify slow queries

**Caching**:
- **Redis Cache**: Cache frequently accessed passengers (by ID)
- **Query Cache**: Cache search results for common queries
- **Application Cache**: Use Spring Cache annotations (@Cacheable)

**Search Engine**:
- **Elasticsearch**: Index passenger data for fast full-text search
- **Synchronization**: Keep search index in sync with database via CDC or polling
- **Fuzzy Matching**: Enable fuzzy search for typos and variations

**API-Level Optimizations**:
- **Rate Limiting**: Prevent abuse of search endpoints
- **Query Complexity Limits**: Restrict number of search parameters
- **Result Size Limits**: Enforce maximum page size
- **Async Processing**: Use async endpoints for complex searches

**Architecture**:
- **Read Replicas**: Offload read queries to database replicas
- **Database Sharding**: Shard passengers by region or user_id
- **Microservices**: Separate passenger search into dedicated service

---

## 11. How would you protect passenger PII?

### General Concept
Personally Identifiable Information (PII) such as names, emails, phone numbers, and passport numbers must be protected through encryption, access control, audit logging, and data minimization.

### Falcon Airlines Implementation (Current State)
- **Authentication**: JWT-based authentication required for all endpoints
- **Authorization**: Method-level security with `@PreAuthorize` annotations
- **DTOs**: Entities not exposed, only approved fields in responses
- **Audit Trail**: Audit fields (created_at, updated_at, created_by, updated_by) track changes

**Additional PII Protection Measures**:

**Encryption**:
- **At Rest**: Encrypt sensitive fields (passport_number, email, phone) in database
- **In Transit**: HTTPS/TLS for all API communications
- **Application-Level**: Use encryption libraries for sensitive data

**Access Control**:
- **Role-Based Access**: Restrict PII access to authorized roles only
- **Field-Level Security**: Redact PII for users without appropriate permissions
- **IP Whitelisting**: Restrict access from trusted IP ranges

**Data Minimization**:
- **Selective Exposure**: Only return PII when necessary
- **Masking**: Mask partial PII in logs and responses (e.g., "J*** D***")
- **Retention Policies**: Automatically delete old passenger records

**Audit Logging**:
- **Access Logs**: Log all PII access with user, timestamp, and reason
- **Change Logs**: Track all modifications to PII fields
- **Alerting**: Alert on suspicious PII access patterns

**Compliance**:
- **GDPR**: Implement right to be forgotten (hard delete for PII)
- **PCI DSS**: Follow payment card industry standards if payment data involved
- **Data Classification**: Classify data by sensitivity level

---

## 12. How would you prevent duplicate passengers?

### General Concept
Preventing duplicate passengers requires a combination of uniqueness constraints, fuzzy matching algorithms, data validation, and user confirmation workflows.

### Falcon Airlines Implementation (Current State)
- **Passport Uniqueness**: UNIQUE constraint on passport_number
- **Email Uniqueness**: Application-level validation for email uniqueness
- **Validation**: Bean Validation for format and constraint validation

**Additional Duplicate Prevention Measures**:

**Fuzzy Matching**:
- **Name Similarity**: Use algorithms like Levenshtein distance to detect similar names
- **DOB Matching**: Check for passengers with same date of birth
- **Phone Matching**: Check for passengers with same phone number
- **Address Matching**: If address data available, check for similar addresses

**Deduplication Service**:
- **Batch Processing**: Run periodic deduplication jobs on existing data
- **Real-Time Checking**: Check for potential duplicates during create/update
- **Merge Workflow**: Allow users to review and merge duplicate records

**User Confirmation**:
- **Duplicate Warning**: Warn users when potential duplicate is detected
- **Confirmation Dialog**: Require user confirmation before creating potential duplicate
- **Link Existing**: Offer to link to existing passenger instead of creating new

**Data Quality**:
- **Normalization**: Normalize phone numbers, emails, and names before comparison
- **Canonical Forms**: Convert data to canonical form for comparison
- **Validation Rules**: Enforce strict validation to prevent variations

**Machine Learning**:
- **Record Linkage**: Use ML models to identify duplicate records
- **Confidence Scores**: Assign confidence scores to potential duplicates
- **Human Review**: Route low-confidence matches for human review

---

## 13. Explain the Passenger module in 60 seconds.

### 60-Second Explanation

The Passenger module manages passenger information in the Falcon Airlines system. It follows a layered architecture with entity-repository-service-controller separation. The Passenger entity stores personal details like name, date of birth, email, phone, passport number, and nationality, with optional linking to a User account. The module uses JPA for persistence with soft delete support via `@SQLRestriction`. 

Validation is implemented at two layers: Bean Validation on the PassengerRequest DTO for format validation (email, phone, passport patterns), and service-layer business validation for uniqueness checks (passport number, email) and referential integrity (preventing deletion if passenger has tickets or bookings). 

The PassengerService provides CRUD operations, search with JPA Specifications for flexible filtering, and passenger history by counting tickets and bookings. The PassengerController exposes REST endpoints at `/api/passengers` with JWT authentication and method-level security using `PASSENGER_READ` and `PASSENGER_WRITE` authorities. DTOs (PassengerRequest and PassengerResponse) separate the API layer from the domain layer, preventing entity exposure and enabling controlled serialization. The module integrates with Ticket and BookingPassenger entities to track passenger travel history.

---

## Summary

The Passenger module in Falcon Airlines demonstrates enterprise-grade design patterns including layered architecture, DTO mapping, comprehensive validation, security through authentication and authorization, and integration with related domain entities. The implementation emphasizes data integrity through uniqueness constraints, referential integrity checks, and soft delete functionality, while providing flexible search capabilities through JPA Specifications and pagination.
