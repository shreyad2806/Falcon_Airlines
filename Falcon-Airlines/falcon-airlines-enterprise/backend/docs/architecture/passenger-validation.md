# Passenger Validation

## Overview
Passenger validation is implemented at two layers in the Falcon Airlines system:
1. **Bean Validation** - Declarative validation on DTOs using Jakarta Bean Validation annotations
2. **Service-Layer Business Validation** - Programmatic validation in the service layer for business rules

This separation ensures that simple format and constraint validation is handled automatically by the framework, while complex business logic remains in the service layer where it can be tested and maintained independently.

## Bean Validation (DTO Layer)

Bean Validation is applied to `PassengerRequest` using Jakarta Bean Validation annotations. This validation occurs automatically when `@Valid` is used on controller method parameters.

### Validation Annotations

#### Required Fields
```java
@NotBlank
@Size(max = 100)
private String firstName;

@NotBlank
@Size(max = 100)
private String lastName;

@NotNull
@Past
private LocalDate dateOfBirth;

@NotNull
private Gender gender;
```

- **@NotBlank**: Ensures the field is not null and not empty (after trimming whitespace)
- **@Size(max = 100)**: Ensures the field does not exceed 100 characters
- **@NotNull**: Ensures the field is not null
- **@Past**: Ensures the date is in the past (for date of birth)

#### Optional Fields with Format Validation
```java
@Email
@Size(max = 255)
private String email;

@Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,20}$", message = "Phone number must be valid")
@Size(max = 20)
private String phone;

@Pattern(regexp = "^[A-Z0-9<]{6,50}$", message = "Passport number must be valid (alphanumeric)")
@Size(max = 50)
private String passportNumber;

@Pattern(regexp = "^[A-Z]{3}$", message = "Nationality must be a valid 3-letter ISO country code")
@Size(max = 3)
private String nationality;

@Size(max = 20)
private String redressNumber;
```

- **@Email**: Ensures the field is a valid email format
- **@Pattern**: Ensures the field matches a regular expression pattern
- **@Size(max = N)**: Ensures the field does not exceed N characters

### Validation Rules

#### Email Validation
- **Annotation**: `@Email`
- **Constraint**: Must be a valid email format (e.g., user@example.com)
- **Max Length**: 255 characters
- **Error**: Returns 400 BAD_REQUEST with validation error message

#### Phone Validation
- **Annotation**: `@Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,20}$")`
- **Pattern Breakdown**:
  - `^\\+?` - Optional leading plus sign
  - `[0-9\\s\\-()]` - Digits, spaces, hyphens, or parentheses
  - `{7,20}` - Between 7 and 20 characters
- **Examples**: `+1234567890`, `123-456-7890`, `(123) 456-7890`
- **Error**: Returns 400 BAD_REQUEST with "Phone number must be valid" message

#### Passport Number Validation
- **Annotation**: `@Pattern(regexp = "^[A-Z0-9<]{6,50}$")`
- **Pattern Breakdown**:
  - `^[A-Z0-9<]` - Uppercase letters, digits, or less-than symbol
  - `{6,50}` - Between 6 and 50 characters
- **Examples**: `AB1234567`, `US12345678`, `P12345678<USA`
- **Error**: Returns 400 BAD_REQUEST with "Passport number must be valid (alphanumeric)" message

#### Date of Birth Validation
- **Annotations**: `@NotNull`, `@Past`
- **Constraint**: Must be a date in the past (cannot be today or future)
- **Error**: Returns 400 BAD_REQUEST with validation error message

#### Nationality Validation
- **Annotation**: `@Pattern(regexp = "^[A-Z]{3}$")`
- **Pattern Breakdown**:
  - `^[A-Z]` - Uppercase letters only
  - `{3}` - Exactly 3 characters
- **Examples**: `USA`, `GBR`, `IND`
- **Error**: Returns 400 BAD_REQUEST with "Nationality must be a valid 3-letter ISO country code" message

#### Gender Validation
- **Annotation**: `@NotNull`
- **Constraint**: Must be one of the enum values: M (Male), F (Female), O (Other)
- **Error**: Returns 400 BAD_REQUEST with validation error message

#### Redress Number Validation
- **Annotation**: `@Size(max = 20)`
- **Constraint**: Maximum 20 characters
- **Error**: Returns 400 BAD_REQUEST with validation error message

#### Emergency Contact
**Note**: The Passenger schema does not contain an emergency contact field. Therefore, no emergency contact validation is implemented.

## Service-Layer Business Validation

Business validation is implemented in `PassengerService` and handles rules that cannot be expressed as simple declarative constraints.

### Uniqueness Validation

#### Passport Number Uniqueness
```java
private void validateUniqueness(PassengerRequest request, Long excludeId) {
    // Check passport number uniqueness
    if (request.getPassportNumber() != null && !request.getPassportNumber().isBlank()) {
        Optional<Passenger> existingByPassport;
        if (excludeId == null) {
            existingByPassport = passengerRepository.findByPassportNumber(request.getPassportNumber());
        } else {
            existingByPassport = passengerRepository.findByPassportNumberAndIdNot(
                    request.getPassportNumber(), excludeId);
        }
        if (existingByPassport.isPresent()) {
            throw new BaseException(HttpStatus.CONFLICT, "DUPLICATE_PASSPORT", 
                    "Passport number already exists");
        }
    }
}
```

- **Purpose**: Ensures no two passengers have the same passport number
- **Implementation**: Queries repository for existing passport number
- **Update Handling**: Excludes current record when updating (using `findByPassportNumberAndIdNot`)
- **Error**: Throws `BaseException` with 409 CONFLICT and code `DUPLICATE_PASSPORT`

#### Email Uniqueness
```java
private void validateUniqueness(PassengerRequest request, Long excludeId) {
    // Check email uniqueness
    if (request.getEmail() != null && !request.getEmail().isBlank()) {
        Optional<Passenger> existingByEmail;
        if (excludeId == null) {
            existingByEmail = passengerRepository.findByEmail(request.getEmail());
        } else {
            existingByEmail = passengerRepository.findByEmailAndIdNot(
                    request.getEmail(), excludeId);
        }
        if (existingByEmail.isPresent()) {
            throw new BaseException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", 
                    "Email already exists");
        }
    }
}
```

- **Purpose**: Ensures no two passengers have the same email address
- **Implementation**: Queries repository for existing email
- **Update Handling**: Excludes current record when updating (using `findByEmailAndIdNot`)
- **Error**: Throws `BaseException` with 409 CONFLICT and code `DUPLICATE_EMAIL`

#### Frequent Flyer Number Validation
**Note**: The Passenger schema does not contain a frequent flyer number field. Therefore, frequent flyer number validation is NOT implemented.

### Existence Validation

#### User Existence
```java
private void validateUserExistence(Long userId) {
    if (userId != null && !userRepository.existsById(userId)) {
        throw new BaseException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", 
                "User not found");
    }
}
```

- **Purpose**: Ensures that if a user ID is provided, the user exists
- **Implementation**: Checks if user exists by ID
- **Error**: Throws `BaseException` with 404 NOT_FOUND and code `USER_NOT_FOUND`

#### Passenger Existence
```java
private Passenger getPassengerOrThrow(Long id) {
    return passengerRepository.findById(id)
            .orElseThrow(() -> new BaseException(HttpStatus.NOT_FOUND, "PASSENGER_NOT_FOUND", 
                    "Passenger not found"));
}
```

- **Purpose**: Ensures passenger exists before update/delete/history operations
- **Implementation**: Queries repository by ID, throws if not found
- **Error**: Throws `BaseException` with 404 NOT_FOUND and code `PASSENGER_NOT_FOUND`

### Referential Integrity Validation

#### Passenger In Use Check
```java
private void validatePassengerNotInUse(Long id) {
    if (ticketRepository.existsByPassengerId(id) || 
        bookingPassengerRepository.existsByPassengerId(id)) {
        throw new BaseException(HttpStatus.CONFLICT, "PASSENGER_IN_USE", 
                "Passenger is referenced by tickets or bookings and cannot be deleted");
    }
}
```

- **Purpose**: Prevents deletion of passengers referenced by tickets or bookings
- **Implementation**: Checks if passenger has tickets or booking-passenger links
- **Error**: Throws `BaseException` with 409 CONFLICT and code `PASSENGER_IN_USE`

## Bean Validation vs Service-Layer Business Validation

### Bean Validation
- **Purpose**: Format and constraint validation
- **Location**: DTO classes (e.g., `PassengerRequest`)
- **Implementation**: Declarative annotations (`@NotBlank`, `@Pattern`, `@Email`, etc.)
- **Execution**: Automatic when `@Valid` is used in controller
- **Use Cases**:
  - Required field checks
  - Format validation (email, phone, passport patterns)
  - Size constraints (max length)
  - Type constraints (past dates, enum values)
- **Advantages**:
  - Declarative and easy to read
  - Automatic execution by framework
  - Standardized error messages
  - No custom code needed for simple rules

### Service-Layer Business Validation
- **Purpose**: Business rule validation
- **Location**: Service classes (e.g., `PassengerService`)
- **Implementation**: Programmatic logic in service methods
- **Execution**: Explicit calls in service methods
- **Use Cases**:
  - Uniqueness checks (passport, email)
  - Existence checks (user, passenger)
  - Referential integrity checks (passenger in use)
  - Complex business rules that span multiple entities
- **Advantages**:
  - Can implement complex logic
  - Can query database for validation
  - Can throw domain-specific exceptions
  - Testable independently of HTTP layer

### When to Use Each

| Scenario | Validation Type | Example |
|----------|----------------|---------|
| Field is required | Bean Validation | `@NotBlank` on firstName |
| Field format must match pattern | Bean Validation | `@Pattern` on phone |
| Field must be within size limit | Bean Validation | `@Size(max = 100)` on name |
| Field must be past date | Bean Validation | `@Past` on dateOfBirth |
| Value must be unique across system | Service Layer | Passport number uniqueness |
| Referenced entity must exist | Service Layer | User existence check |
| Entity cannot be deleted if referenced | Service Layer | Passenger in use check |
| Complex cross-entity business rule | Service Layer | Booking availability check |

## Validation Flow

### Create Passenger Flow
1. Controller receives `PassengerRequest` with `@Valid` annotation
2. Bean Validation executes automatically:
   - Checks required fields (firstName, lastName, dateOfBirth, gender)
   - Validates email format if provided
   - Validates phone pattern if provided
   - Validates passport pattern if provided
   - Validates nationality pattern if provided
3. If Bean Validation fails → 400 BAD_REQUEST
4. If Bean Validation passes → Service layer executes:
   - Validates user existence (if userId provided)
   - Validates passport number uniqueness (if provided)
   - Validates email uniqueness (if provided)
5. If Service Validation fails → 404 NOT_FOUND or 409 CONFLICT
6. If all validations pass → Passenger is created

### Update Passenger Flow
1. Controller receives `PassengerRequest` with `@Valid` annotation
2. Bean Validation executes automatically (same as create)
3. If Bean Validation fails → 400 BAD_REQUEST
4. If Bean Validation passes → Service layer executes:
   - Fetches existing passenger (throws if not found)
   - Validates user existence (if userId provided)
   - Validates passport number uniqueness (excludes current record)
   - Validates email uniqueness (excludes current record)
5. If Service Validation fails → 404 NOT_FOUND or 409 CONFLICT
6. If all validations pass → Passenger is updated

### Delete Passenger Flow
1. Controller receives passenger ID
2. Service layer executes:
   - Fetches existing passenger (throws if not found)
   - Validates passenger not in use (no tickets or booking-passenger links)
3. If Service Validation fails → 404 NOT_FOUND or 409 CONFLICT
4. If all validations pass → Passenger is soft deleted

## Error Handling

All validation errors are handled by `GlobalExceptionHandler`:

- **Bean Validation Errors**: 400 BAD_REQUEST with field-specific error messages
- **Service Validation Errors**: Domain-specific HTTP status codes:
  - 404 NOT_FOUND: Passenger or user not found
  - 409 CONFLICT: Duplicate passport/email, passenger in use

## Testing

Validation is tested at multiple levels:

1. **DTO Validation Tests** (`PassengerRequestValidationTest`): 21 tests
   - Tests all Bean Validation annotations
   - Tests invalid email, phone, passport formats
   - Tests missing required fields
   - Tests future DOB

2. **Service Unit Tests** (`PassengerServiceTest`): 21 tests
   - Tests duplicate passport validation
   - Tests duplicate email validation
   - Tests user not found validation
   - Tests passenger not found validation
   - Tests passenger in use validation

3. **Controller Integration Tests** (`PassengerControllerIntegrationTest`): 24 tests
   - Tests 400 BAD_REQUEST for invalid data
   - Tests 404 NOT_FOUND for missing entities
   - Tests 409 CONFLICT for duplicates
   - Tests 401 UNAUTHORIZED for missing token
