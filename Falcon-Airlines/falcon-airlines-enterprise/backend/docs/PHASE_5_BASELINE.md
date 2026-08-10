# Phase 5 Passenger Management - Baseline Audit

## IMPLEMENTED

### Entity Layer
- **Passenger entity** (`com.falcon.airlines.entity.Passenger`)
  - Extends AuditEntity (soft delete via @SQLRestriction)
  - @ManyToOne relationship to User (nullable)
  - Fields: userId, firstName, lastName, dateOfBirth, email, phone, passportNumber, nationality, gender, redressNumber
  - Uses Gender enum (M, F, X, O)
- **BookingPassenger entity** (`com.falcon.airlines.entity.BookingPassenger`)
  - Junction table between Booking and Passenger
  - @ManyToOne to Booking (nullable=false)
  - @ManyToOne to Passenger (nullable=false)
  - Fields: fareClass, cabin, ssrCodes (array)

### Repository Layer
- **PassengerRepository** (`com.falcon.airlines.repository.PassengerRepository`)
  - Extends JpaRepository<Passenger, Long>
  - Extends JpaSpecificationExecutor<Passenger> (supports dynamic queries)
  - No custom methods defined
- **BookingPassengerRepository** (`com.falcon.airlines.repository.BookingPassengerRepository`)
  - Extends JpaRepository<BookingPassenger, Long>

### DTO Layer
- **PassengerRequest** (`com.falcon.airlines.dto.request.PassengerRequest`)
  - Validation: @NotBlank (firstName, lastName), @NotNull (dateOfBirth, gender), @Past (dateOfBirth), @Email (email), @Size on various fields
  - Maps userId to user.id in mapper
- **PassengerResponse** (`com.falcon.airlines.dto.response.PassengerResponse`)
  - Includes id, userId, all passenger fields, createdAt, updatedAt

### Mapper Layer
- **PassengerMapper** (`com.falcon.airlines.mapper.PassengerMapper`)
  - MapStruct interface with @Mapping annotations
  - toEntity: maps userId to user.id
  - toResponse: maps user.id to userId

### Database Schema
- **passengers table** (V2__create_schema.sql)
  - Columns: id, user_id (FK), first_name, last_name, date_of_birth, email, phone, passport_number, nationality, gender (CHECK constraint), redress_number, audit columns
  - Indexes: idx_passengers_user (user_id), idx_passengers_name_dob (last_name, first_name, date_of_birth)
- **booking_passengers table** (V2__create_schema.sql)
  - Columns: id, booking_id (FK), passenger_id (FK), fare_class, cabin, ssr_codes (array), audit columns

### Security/Permissions
- **Permission enum** includes PASSENGER_READ and PASSENGER_WRITE
- **Seed data** (V3__seed_reference_data.sql) seeds these permissions
- Role hierarchy: ROLE_ADMIN > ROLE_AGENT > ROLE_CUSTOMER

### Related Entities
- **User entity** - has relationship to Passenger via user_id FK
- **Booking entity** - has customer relationship to User
- **Ticket entity** - has relationship to Passenger via passenger_id FK

### Exception Handling
- **BaseException** - custom exception with HttpStatus and errorCode
- **GlobalExceptionHandler** - converts exceptions to ApiErrorResponse
- **ApiErrorResponse** - standardized error response with traceId

### Response Format
- **ApiResponse<T>** - wrapper for successful responses with success flag, message, data, timestamp

### Conventions (from Airport/Aircraft/Flight modules)
- **Pagination**: Spring Data Pageable interface
- **Search**: JPA Specifications for dynamic queries
- **Validation**: Jakarta Validation annotations on DTOs
- **Security**: @PreAuthorize with hasAuthority() or hasRole()
- **Soft delete**: @SQLRestriction("is_deleted = false") on entities
- **Transactional**: @Transactional on service methods
- **Controller**: @RestController, @RequestMapping, @Tag for Swagger
- **Swagger**: @Operation, @SecurityRequirement(name = "bearerAuth")

### Test Infrastructure
- **BaseUnitTest** - base class for unit tests with Mockito
- **Test patterns**: AirportServiceTest, AirportControllerTest, FlightControllerIntegrationTest
- **Testcontainers**: BaseIntegrationTest for integration tests

---

## MISSING

### Service Layer
- **PassengerService** - does NOT exist
  - Need CRUD operations (create, getById, update, delete)
  - Need search/filtering (by name, email, passport, userId)
  - Need pagination support
  - Need uniqueness validation (passport number?)
  - Need referential integrity checks (before delete if referenced by tickets/booking_passengers)

### Controller Layer
- **PassengerController** - does NOT exist
  - Need REST endpoints at /api/passengers
  - Need GET /api/passengers (list/search with pagination)
  - Need GET /api/passengers/{id}
  - Need POST /api/passengers
  - Need PUT /api/passengers/{id}
  - Need DELETE /api/passengers/{id}
  - Need security annotations (@PreAuthorize)
  - Need Swagger annotations (@Operation, @SecurityRequirement)

### DTO Layer
- **TravelDocumentRequest** - does NOT exist (travel_documents table exists in schema but no entity/DTO)
- **TravelDocumentResponse** - does NOT exist
- **TravelDocumentMapper** - does NOT exist

### Entity Layer
- **TravelDocument entity** - does NOT exist (table defined in schema but no entity class)

### Repository Layer
- **TravelDocumentRepository** - does NOT exist

### Tests
- **PassengerServiceTest** - does NOT exist
- **PassengerControllerTest** - does NOT exist
- **PassengerRepositoryIntegrationTest** - does NOT exist
- **PassengerControllerIntegrationTest** - does NOT exist

---

## REUSABLE

### Patterns from Airport/Aircraft/Flight Modules
- **Service pattern**: Follow AirportService structure
  - createX(Request): validate, map, save, return response
  - getXById(id): findById or throw NOT_FOUND
  - updateX(id, Request): findById, validate, update, save
  - deleteX(id): check references, soft delete
  - listX(Pageable): delegate to search
  - searchX(filters, Pageable): build specification, query, map to response
- **Controller pattern**: Follow AirportController structure
  - @RestController, @RequestMapping("/api/passengers")
  - @Tag for Swagger
  - @SecurityRequirement(name = "bearerAuth") on all methods
  - @PreAuthorize on each method
  - @Valid on @RequestBody
  - ResponseEntity<ApiResponse<T>> return type
- **Specification pattern**: Follow AirportService.buildSpecification()
  - Dynamic query building with optional filters
  - Case-insensitive LIKE for text fields
  - Exact match for enums/IDs
- **Validation pattern**: Follow AirportRequest
  - @NotBlank for required strings
  - @Size for length constraints
  - @NotNull for required fields
  - Custom validation in service layer (uniqueness, business rules)
- **Exception pattern**: Use BaseException with appropriate HttpStatus and errorCode
  - NOT_FOUND (404) for missing entities
  - CONFLICT (409) for duplicates/referential integrity violations
  - BAD_REQUEST (400) for validation failures

### Existing Infrastructure
- **Permission enum**: PASSENGER_READ, PASSENGER_WRITE already defined
- **Database schema**: passengers table already created with indexes
- **Mapper pattern**: PassengerMapper already exists
- **DTO pattern**: PassengerRequest/Response already exist
- **Response wrapper**: ApiResponse<T> already exists
- **Exception handling**: GlobalExceptionHandler already handles BaseException
- **Security**: JWT authentication and role hierarchy already configured
- **Test base classes**: BaseUnitTest, BaseIntegrationTest already exist

---

## POTENTIAL CONFLICTS

### None Identified
- No naming conflicts with existing classes
- Passenger entity and repository already exist and follow conventions
- Permissions already defined and seeded
- Database schema already aligned with entity

### Considerations
- **User relationship**: Passenger.user is nullable - need to decide if passengers can exist without associated users
- **Delete behavior**: Need to check if passenger is referenced by tickets or booking_passengers before soft delete
- **Uniqueness**: No uniqueness constraint on passport_number in database - may need application-level validation if required
- **Search scope**: Decide if search should include user-related filters (e.g., search passengers by user_id)

---

## RECOMMENDED IMPLEMENTATION ORDER

1. **PassengerService** (highest priority - core business logic)
   - Implement CRUD operations following AirportService pattern
   - Implement search/filtering with JPA Specifications
   - Add uniqueness validation (if needed for passport)
   - Add referential integrity check before delete (check tickets, booking_passengers)
   - Use PassengerMapper for entity/DTO conversion

2. **PassengerController** (expose REST API)
   - Implement REST endpoints following AirportController pattern
   - Add @PreAuthorize with PASSENGER_READ/PASSENGER_WRITE
   - Add Swagger annotations
   - Use ApiResponse<T> wrapper

3. **PassengerServiceTest** (unit tests)
   - Follow AirportServiceTest pattern
   - Test CRUD operations
   - Test validation and error cases
   - Test search/filtering
   - Mock repository and mapper

4. **PassengerControllerTest** (controller tests)
   - Follow AirportControllerTest pattern
   - Test endpoint security
   - Test request validation
   - Test response format
   - Mock PassengerService

5. **PassengerRepositoryIntegrationTest** (repository tests)
   - Test custom queries if added
   - Test soft delete behavior
   - Use Testcontainers

6. **PassengerControllerIntegrationTest** (integration tests)
   - Follow FlightControllerIntegrationTest pattern
   - Test full request/response cycle
   - Test authentication/authorization
   - Use TestRestTemplate

### Optional/Deferred
- **TravelDocument module** - defer unless explicitly required for Phase 5
  - TravelDocument entity, repository, service, controller, DTOs, tests
  - Relationship to Passenger already defined in schema

### Naming Conventions to Follow
- **Service methods**: createPassenger, getPassengerById, updatePassenger, deletePassenger, listPassengers, searchPassengers
- **Controller endpoints**: GET /api/passengers, GET /api/passengers/{id}, POST /api/passengers, PUT /api/passengers/{id}, DELETE /api/passengers/{id}
- **Error codes**: PASSENGER_NOT_FOUND, PASSENGER_IN_USE, DUPLICATE_PASSPORT_NUMBER (if needed)
- **Search parameters**: firstName, lastName, email, passportNumber, userId, dateOfBirth (range)

### Security Recommendations
- **Read operations**: @PreAuthorize("hasAnyAuthority('PASSENGER_READ')")
- **Write operations**: @PreAuthorize("hasAnyAuthority('PASSENGER_WRITE')")
- **Self-service**: Consider allowing users to manage their own passengers (user.id == passenger.user.id)
