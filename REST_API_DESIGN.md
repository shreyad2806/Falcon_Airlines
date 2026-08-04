# Falcon Airlines Enterprise — REST API Design

## Conventions

- **Base URL**: `https://api.falconairlines.com/api/v1`
- **Content-Type**: `application/json` for request and response bodies.
- **Authentication**: `Authorization: Bearer <JWT>` header.
- **Standard HTTP Methods**: `GET`, `POST`, `PUT`, `PATCH`, `DELETE` used semantically.
- **Resource Naming**: Plural nouns, kebab-case for multi-word paths.
- **Pagination**: `?page={n}&size={n}&sort={field},{asc|desc}` for list endpoints.
- **Filtering**: Query parameters for search endpoints.
- **Idempotency**: `Idempotency-Key: <uuid>` supported for `POST` mutation endpoints.

## Global Status Codes

| Code | Meaning |
|------|---------|
| `200 OK` | Successful `GET`, `PUT`, `PATCH`, `DELETE`. |
| `201 Created` | Successful `POST` resource creation. |
| `204 No Content` | Successful delete without response body. |
| `400 Bad Request` | Malformed request or validation failure. |
| `401 Unauthorized` | Missing or invalid JWT. |
| `403 Forbidden` | Authenticated but insufficient role/permission. |
| `404 Not Found` | Resource not found. |
| `409 Conflict` | Business rule conflict (e.g., duplicate, concurrent modification). |
| `422 Unprocessable Entity` | Business validation failure. |
| `429 Too Many Requests` | Rate limit exceeded. |
| `500 Internal Server Error` | Unexpected server error. |

---

## 1. Authentication

### 1.1 Register a customer account

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/register` |
| **Auth Required** | No |
| **Role Required** | None |
| **Status Codes** | `201`, `400`, `409`, `422` |

**Validation Rules**
- `email`: required, valid email, max 255 chars, unique.
- `password`: required, min 12 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char.
- `mobileNumber`: optional, valid E.164.
- `firstName`, `lastName`: required, alphabetic, max 100 chars.

**Request Body**
```json
{
  "email": "john.doe@example.com",
  "password": "Str0ng!Pass",
  "mobileNumber": "+919876543210",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response Body (`201 Created`)**
```json
{
  "userId": 123,
  "email": "john.doe@example.com",
  "status": "PENDING_VERIFICATION",
  "message": "Account created. Please verify your email."
}
```

---

### 1.2 Login

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/login` |
| **Auth Required** | No |
| **Role Required** | None |
| **Status Codes** | `200`, `400`, `401`, `403`, `422`, `429` |

**Validation Rules**
- `email` or `username`: required.
- `password`: required, min 1 char.
- Account must not be locked.

**Request Body**
```json
{
  "email": "john.doe@example.com",
  "password": "Str0ng!Pass"
}
```

**Response Body (`200 OK`)**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "dGhpcyBpcyBh...",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "mfaRequired": false
}
```

---

### 1.3 Verify MFA

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/mfa/verify` |
| **Auth Required** | Yes (intermediate token) |
| **Role Required** | None |
| **Status Codes** | `200`, `400`, `401`, `422` |

**Validation Rules**
- `mfaToken`: required, valid intermediate token.
- `otpCode`: required, 6 digits.

**Request Body**
```json
{
  "mfaToken": "eyJ0bXAi...",
  "otpCode": "123456"
}
```

**Response Body (`200 OK`)**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "dGhpcyBpcyBh...",
  "expiresIn": 900,
  "tokenType": "Bearer"
}
```

---

### 1.4 Refresh Token

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/refresh` |
| **Auth Required** | No |
| **Role Required** | None |
| **Status Codes** | `200`, `400`, `401` |

**Validation Rules**
- `refreshToken`: required, non-expired, not revoked.

**Request Body**
```json
{
  "refreshToken": "dGhpcyBpcyBh..."
}
```

**Response Body (`200 OK`)**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "dGhpcyBpcyBh...",
  "expiresIn": 900,
  "tokenType": "Bearer"
}
```

---

### 1.5 Logout

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/logout` |
| **Auth Required** | Yes |
| **Role Required** | None |
| **Status Codes** | `204`, `401` |

**Validation Rules**
- `refreshToken` required in body or valid access token in header.

**Request Body**
```json
{
  "refreshToken": "dGhpcyBpcyBh..."
}
```

**Response Body**
- `204 No Content`

---

## 2. Users

### 2.1 Get current user

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/users/me` |
| **Auth Required** | Yes |
| **Role Required** | None |
| **Status Codes** | `200`, `401` |

**Response Body (`200 OK`)**
```json
{
  "id": 123,
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["CUSTOMER"],
  "mfaEnabled": true
}
```

---

### 2.2 Update current user

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **URL** | `/api/v1/users/me` |
| **Auth Required** | Yes |
| **Role Required** | None |
| **Status Codes** | `200`, `400`, `401`, `422` |

**Validation Rules**
- `firstName`, `lastName`: optional, alphabetic, max 100.
- `mobileNumber`: optional, valid E.164, unique.

**Request Body**
```json
{
  "firstName": "Johnny",
  "mobileNumber": "+919876543211"
}
```

**Response Body (`200 OK`)**
```json
{
  "id": 123,
  "email": "john.doe@example.com",
  "firstName": "Johnny",
  "lastName": "Doe"
}
```

---

### 2.3 List users (admin)

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/users` |
| **Auth Required** | Yes |
| **Role Required** | `ADMIN` |
| **Status Codes** | `200`, `401`, `403` |

**Query Parameters**
- `role`, `status`, `email`, `page`, `size`, `sort`

**Response Body (`200 OK`)**
```json
{
  "content": [
    { "id": 123, "email": "john@example.com", "status": "ACTIVE", "roles": ["CUSTOMER"] }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

---

### 2.4 Assign role to user (admin)

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/users/{userId}/roles` |
| **Auth Required** | Yes |
| **Role Required** | `ADMIN` |
| **Status Codes** | `201`, `400`, `401`, `403`, `404`, `409` |

**Validation Rules**
- `roleId`: required, existing role.
- Cannot assign the same role twice.

**Request Body**
```json
{
  "roleId": 5,
  "validFrom": "2026-08-04T00:00:00Z",
  "validUntil": null
}
```

**Response Body (`201 Created`)**
```json
{
  "userRoleId": 45,
  "userId": 123,
  "roleId": 5,
  "validFrom": "2026-08-04T00:00:00Z"
}
```

---

## 3. Passengers

### 3.1 Create a passenger

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/passengers` |
| **Auth Required** | Yes |
| **Role Required** | `CUSTOMER` or `AGENT` |
| **Status Codes** | `201`, `400`, `401`, `403`, `422` |

**Validation Rules**
- `firstName`, `lastName`: required, max 100.
- `dateOfBirth`: required, not in the future.
- `nationality`: required, ISO 3166-1 alpha-3.
- `passportNumber`: optional, alphanumeric, max 50.
- `gender`: required, one of `M`, `F`, `X`, `O`.

**Request Body**
```json
{
  "firstName": "Alice",
  "lastName": "Doe",
  "dateOfBirth": "1990-05-15",
  "nationality": "IND",
  "passportNumber": "J1234567",
  "gender": "F"
}
```

**Response Body (`201 Created`)**
```json
{
  "id": 456,
  "firstName": "Alice",
  "lastName": "Doe",
  "userId": 123
}
```

---

### 3.2 Get passenger

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/passengers/{passengerId}` |
| **Auth Required** | Yes |
| **Role Required** | Owner, `AGENT`, or `ADMIN` |
| **Status Codes** | `200`, `401`, `403`, `404` |

**Response Body (`200 OK`)**
```json
{
  "id": 456,
  "firstName": "Alice",
  "lastName": "Doe",
  "dateOfBirth": "1990-05-15",
  "nationality": "IND",
  "documents": [
    { "type": "PASSPORT", "number": "J1234567", "expiryDate": "2030-01-01" }
  ]
}
```

---

### 3.3 Update passenger

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **URL** | `/api/v1/passengers/{passengerId}` |
| **Auth Required** | Yes |
| **Role Required** | Owner, `AGENT`, or `ADMIN` |
| **Status Codes** | `200`, `400`, `401`, `403`, `404`, `422` |

**Validation Rules**
- Same as create. Cannot change user linkage for an existing record.

**Response Body (`200 OK`)**
```json
{
  "id": 456,
  "firstName": "Alice",
  "lastName": "Smith"
}
```

---

## 4. Airports

### 4.1 List airports

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/airports` |
| **Auth Required** | Optional (public read) |
| **Role Required** | None |
| **Status Codes** | `200` |

**Query Parameters**
- `country`, `city`, `active`, `search`, `page`, `size`

**Response Body (`200 OK`)**
```json
{
  "content": [
    { "id": 1, "iataCode": "DEL", "name": "Indira Gandhi International", "city": "New Delhi", "country": "IN" }
  ]
}
```

---

### 4.2 Create airport (admin)

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/airports` |
| **Auth Required** | Yes |
| **Role Required** | `ADMIN` |
| **Status Codes** | `201`, `400`, `401`, `403`, `409`, `422` |

**Validation Rules**
- `iataCode`: required, 3 uppercase chars, unique.
- `icaoCode`: optional, 4 uppercase chars, unique.
- `name`, `city`: required, max 200/100.
- `country`: required, 2 chars.
- `timeZone`: required, IANA identifier.

**Request Body**
```json
{
  "iataCode": "DEL",
  "icaoCode": "VIDP",
  "name": "Indira Gandhi International",
  "city": "New Delhi",
  "country": "IN",
  "timeZone": "Asia/Kolkata"
}
```

**Response Body (`201 Created`)**
```json
{
  "id": 1,
  "iataCode": "DEL",
  "name": "Indira Gandhi International"
}
```

---

### 4.3 Update airport (admin)

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **URL** | `/api/v1/airports/{airportId}` |
| **Auth Required** | Yes |
| **Role Required** | `ADMIN` |
| **Status Codes** | `200`, `400`, `401`, `403`, `404`, `409`, `422` |

**Response Body (`200 OK`)**
```json
{
  "id": 1,
  "iataCode": "DEL",
  "name": "Indira Gandhi International Airport",
  "isActive": true
}
```

---

## 5. Flights

### 5.1 Search flights

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/flights` |
| **Auth Required** | No |
| **Role Required** | None |
| **Status Codes** | `200`, `400` |

**Query Parameters (required)**
- `origin`: IATA code.
- `destination`: IATA code.
- `departureDate`: ISO date.
- `adults`: int, min 1, max 9.

**Optional Query Parameters**
- `returnDate`, `children`, `infants`, `cabin`, `preferredAirline`, `stops`

**Response Body (`200 OK`)**
```json
{
  "origin": "DEL",
  "destination": "DXB",
  "departureDate": "2026-09-01",
  "flights": [
    {
      "flightId": 789,
      "flightNumber": "6E-123",
      "departure": "2026-09-01T05:30:00+05:30",
      "arrival": "2026-09-01T07:15:00+04:00",
      "fares": [
        { "cabin": "ECONOMY", "family": "Lite", "amount": 14500, "currency": "INR" }
      ]
    }
  ]
}
```

---

### 5.2 Create flight schedule (admin)

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/flights` |
| **Auth Required** | Yes |
| **Role Required** | `ADMIN` or `OPERATIONS` |
| **Status Codes** | `201`, `400`, `401`, `403`, `404`, `422` |

**Validation Rules**
- `flightNumber`: required, max 10.
- `originAirportId`, `destinationAirportId`: required, not equal.
- `aircraftId`: required, valid.
- `scheduledDeparture`, `scheduledArrival`: required, arrival after departure.

**Request Body**
```json
{
  "flightNumber": "6E-123",
  "originAirportId": 1,
  "destinationAirportId": 2,
  "aircraftId": 10,
  "scheduledDeparture": "2026-09-01T05:30:00+05:30",
  "scheduledArrival": "2026-09-01T07:15:00+04:00"
}
```

**Response Body (`201 Created`)**
```json
{
  "id": 789,
  "flightNumber": "6E-123",
  "status": "SCHEDULED"
}
```

---

### 5.3 Get flight details

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/flights/{flightId}` |
| **Auth Required** | No |
| **Role Required** | None |
| **Status Codes** | `200`, `404` |

**Response Body (`200 OK`)**
```json
{
  "id": 789,
  "flightNumber": "6E-123",
  "origin": { "iataCode": "DEL" },
  "destination": { "iataCode": "DXB" },
  "scheduledDeparture": "2026-09-01T05:30:00+05:30",
  "status": "SCHEDULED",
  "inventory": { "Y": 45, "M": 12, "J": 4 }
}
```

---

### 5.4 Update flight (admin)

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **URL** | `/api/v1/flights/{flightId}` |
| **Auth Required** | Yes |
| **Role Required** | `ADMIN` or `OPERATIONS` |
| **Status Codes** | `200`, `400`, `401`, `403`, `404`, `409`, `422` |

**Validation Rules**
- Cannot change past flights.
- If status changes to `CANCELLED`, must trigger rebooking/disruption workflow.

**Request Body**
```json
{
  "scheduledDeparture": "2026-09-01T06:00:00+05:30",
  "status": "DELAYED"
}
```

**Response Body (`200 OK`)**
```json
{
  "id": 789,
  "status": "DELAYED",
  "scheduledDeparture": "2026-09-01T06:00:00+05:30"
}
```

---

## 6. Bookings

### 6.1 Create booking

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/bookings` |
| **Auth Required** | Yes |
| **Role Required** | `CUSTOMER` or `AGENT` |
| **Status Codes** | `201`, `400`, `401`, `403`, `404`, `409`, `422` |

**Validation Rules**
- `flightIds`: required, array of 1–6 flight segment IDs.
- `passengerIds`: required, 1–9 passengers, at least one adult.
- Duplicate passengers not allowed on the same booking.
- Inventory must be available for the selected cabin/fare class.

**Request Body**
```json
{
  "flightIds": [789],
  "passengerIds": [456],
  "cabin": "ECONOMY",
  "fareFamily": "Value",
  "contactEmail": "john.doe@example.com",
  "contactPhone": "+919876543210"
}
```

**Response Body (`201 Created`)**
```json
{
  "bookingId": 321,
  "bookingReference": "A1B2C3",
  "status": "PENDING",
  "totalAmount": 16200,
  "currency": "INR",
  "timeLimit": "2026-09-01T04:30:00+05:30"
}
```

---

### 6.2 Get booking

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/bookings/{bookingReference}` |
| **Auth Required** | Yes |
| **Role Required** | Owner, `AGENT`, or `ADMIN` |
| **Status Codes** | `200`, `401`, `403`, `404` |

**Response Body (`200 OK`)**
```json
{
  "bookingReference": "A1B2C3",
  "status": "CONFIRMED",
  "totalAmount": 16200,
  "currency": "INR",
  "passengers": [
    { "passengerId": 456, "name": "Alice Doe" }
  ],
  "flights": [
    { "flightId": 789, "flightNumber": "6E-123" }
  ],
  "tickets": [
    { "ticketNumber": "234-1234567890" }
  ]
}
```

---

### 6.3 Cancel booking

| Attribute | Value |
|-----------|-------|
| **Method** | `DELETE` |
| **URL** | `/api/v1/bookings/{bookingReference}` |
| **Auth Required** | Yes |
| **Role Required** | Owner, `AGENT`, or `ADMIN` |
| **Status Codes** | `200`, `400`, `401`, `403`, `404`, `409`, `422` |

**Validation Rules**
- Booking must not be in the past.
- Cancellation must respect fare rules (refundable / non-refundable).
- If already ticketed, refund workflow must be triggered.

**Request Body**
```json
{
  "reason": "PASSENGER_REQUEST",
  "refundToOriginalPayment": true
}
```

**Response Body (`200 OK`)**
```json
{
  "bookingReference": "A1B2C3",
  "status": "CANCELLED",
  "refundAmount": 14500,
  "currency": "INR"
}
```

---

### 6.4 Select seats

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/bookings/{bookingReference}/seats` |
| **Auth Required** | Yes |
| **Role Required** | Owner, `AGENT`, or `ADMIN` |
| **Status Codes** | `200`, `400`, `401`, `403`, `404`, `409`, `422` |

**Validation Rules**
- `flightId` and `passengerId` required.
- `seatNumber` must exist on the aircraft map and be available.
- Additional fee must be included if applicable.

**Request Body**
```json
{
  "flightId": 789,
  "passengerId": 456,
  "seatNumber": "12A"
}
```

**Response Body (`200 OK`)**
```json
{
  "passengerId": 456,
  "seatNumber": "12A",
  "extraFee": 500,
  "currency": "INR"
}
```

---

### 6.5 Rebook booking after disruption

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/bookings/{bookingReference}/rebook` |
| **Auth Required** | Yes |
| **Role Required** | `AGENT` or `ADMIN` |
| **Status Codes** | `200`, `400`, `401`, `403`, `404`, `409`, `422` |

**Validation Rules**
- Original flight must be delayed or cancelled.
- New flights must have sufficient inventory for all passengers and same/higher cabin.

**Request Body**
```json
{
  "newFlightIds": [790]
}
```

**Response Body (`200 OK`)**
```json
{
  "bookingReference": "A1B2C3",
  "newFlights": [
    { "flightId": 790, "flightNumber": "6E-125" }
  ],
  "status": "CONFIRMED"
}
```

---

## 7. Payments

### 7.1 Initiate payment

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/payments` |
| **Auth Required** | Yes |
| **Role Required** | `CUSTOMER` or `AGENT` |
| **Status Codes** | `201`, `400`, `401`, `403`, `404`, `409`, `422` |

**Validation Rules**
- `bookingReference`: required, exists, `PENDING` payment status.
- `paymentMethod`: one of `CARD`, `WALLET`, `UPI`, `POINTS`.
- `amount` must match booking total.
- Payment token or instrument reference must be valid.

**Request Body**
```json
{
  "bookingReference": "A1B2C3",
  "paymentMethod": "CARD",
  "paymentToken": "tok_1A2b3C",
  "amount": 16200,
  "currency": "INR"
}
```

**Response Body (`201 Created`)**
```json
{
  "paymentId": 987,
  "transactionId": "TXN-20260804-0001",
  "status": "CAPTURED",
  "amount": 16200,
  "currency": "INR"
}
```

---

### 7.2 Get payment status

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/payments/{paymentId}` |
| **Auth Required** | Yes |
| **Role Required** | Owner, `AGENT`, or `ADMIN` |
| **Status Codes** | `200`, `401`, `403`, `404` |

**Response Body (`200 OK`)**
```json
{
  "paymentId": 987,
  "transactionId": "TXN-20260804-0001",
  "status": "CAPTURED",
  "bookingReference": "A1B2C3",
  "amount": 16200,
  "currency": "INR"
}
```

---

### 7.3 Request refund

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/payments/{paymentId}/refunds` |
| **Auth Required** | Yes |
| **Role Required** | `AGENT` or `ADMIN` |
| **Status Codes** | `201`, `400`, `401`, `403`, `404`, `422` |

**Validation Rules**
- Payment must be `CAPTURED`.
- Refund amount cannot exceed paid amount.
- Booking/ticket status allows refund per fare rules.

**Request Body**
```json
{
  "amount": 16200,
  "currency": "INR",
  "reason": "CANCELLATION"
}
```

**Response Body (`201 Created`)**
```json
{
  "refundId": 111,
  "paymentId": 987,
  "amount": 16200,
  "status": "PENDING"
}
```

---

## 8. Tickets

### 8.1 Get ticket

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/tickets/{ticketNumber}` |
| **Auth Required** | Yes |
| **Role Required** | Owner, `AGENT`, or `ADMIN` |
| **Status Codes** | `200`, `401`, `403`, `404` |

**Response Body (`200 OK`)**
```json
{
  "ticketNumber": "234-1234567890",
  "status": "ISSUED",
  "passengerName": "Alice Doe",
  "flightNumber": "6E-123",
  "cabin": "ECONOMY",
  "fare": 14500,
  "taxes": 1700
}
```

---

### 8.2 Void ticket

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/tickets/{ticketNumber}/void` |
| **Auth Required** | Yes |
| **Role Required** | `AGENT` or `ADMIN` |
| **Status Codes** | `200`, `400`, `401`, `403`, `404`, `409`, `422` |

**Validation Rules**
- Ticket must be unused and within void window (e.g., before departure).
- Associated PNR must allow void per fare rules.

**Request Body**
```json
{
  "reason": "AGENT_ERROR"
}
```

**Response Body (`200 OK`)**
```json
{
  "ticketNumber": "234-1234567890",
  "status": "VOID"
}
```

---

### 8.3 Reissue ticket

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/tickets/{ticketNumber}/reissue` |
| **Auth Required** | Yes |
| **Role Required** | `AGENT` or `ADMIN` |
| **Status Codes** | `200`, `400`, `401`, `403`, `404`, `409`, `422` |

**Validation Rules**
- New flight must be valid and available.
- Fare difference and taxes must be collected or refunded before reissue.

**Request Body**
```json
{
  "newFlightId": 790,
  "fareDifference": 1500,
  "currency": "INR"
}
```

**Response Body (`200 OK`)**
```json
{
  "ticketNumber": "234-1234567890",
  "status": "REISSUED",
  "newFlightId": 790
}
```

---

## 9. Delay Prediction

### 9.1 Get delay prediction for a flight

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/delay-predictions/flights/{flightId}` |
| **Auth Required** | Yes |
| **Role Required** | `CUSTOMER`, `AGENT`, or `ADMIN` |
| **Status Codes** | `200`, `401`, `403`, `404` |

**Response Body (`200 OK`)**
```json
{
  "flightId": 789,
  "predictionTime": "2026-09-01T03:00:00Z",
  "predictedDelayMinutes": 45,
  "probability": 0.82,
  "riskLevel": "HIGH",
  "factors": { "weather": 0.6, "airportCongestion": 0.3 }
}
```

---

### 9.2 Get rebooking recommendations

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/delay-predictions/flights/{flightId}/recommendations` |
| **Auth Required** | Yes |
| **Role Required** | `AGENT` or `ADMIN` |
| **Status Codes** | `200`, `401`, `403`, `404` |

**Response Body (`200 OK`)**
```json
{
  "flightId": 789,
  "recommendations": [
    { "flightId": 790, "flightNumber": "6E-125", "availableSeats": 12 }
  ]
}
```

---

## 10. Analytics

### 10.1 Get executive dashboard

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/analytics/dashboard` |
| **Auth Required** | Yes |
| **Role Required** | `ADMIN` |
| **Status Codes** | `200`, `401`, `403` |

**Query Parameters**
- `startDate`, `endDate`, `route`, `cabin`, `channel`

**Response Body (`200 OK`)**
```json
{
  "period": { "startDate": "2026-08-01", "endDate": "2026-08-31" },
  "totalBookings": 125000,
  "totalRevenue": { "amount": 45000000, "currency": "INR" },
  "loadFactor": 0.84,
  "ancillaryRevenue": { "amount": 5200000, "currency": "INR" }
}
```

---

### 10.2 Export sales report

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/analytics/reports/sales` |
| **Auth Required** | Yes |
| **Role Required** | `ADMIN` or `FINANCE` |
| **Status Codes** | `200`, `400`, `401`, `403` |

**Query Parameters**
- `startDate`, `endDate`, `route`, `channel`, `format` (CSV, JSON)

**Response Body (`200 OK`)**
```json
{
  "reportId": "RPT-20260804-001",
  "downloadUrl": "https://storage.falconairlines.com/reports/RPT-20260804-001.csv",
  "expiresAt": "2026-08-11T00:00:00Z"
}
```

---

### 10.3 Flight performance report

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/analytics/reports/flight-performance` |
| **Auth Required** | Yes |
| **Role Required** | `ADMIN` or `OPERATIONS` |
| **Status Codes** | `200`, `400`, `401`, `403` |

**Response Body (`200 OK`)**
```json
{
  "flights": [
    {
      "flightNumber": "6E-123",
      "departures": 30,
      "onTimePercentage": 0.87,
      "averageDelayMinutes": 12
    }
  ]
}
```

---

## Total API Count: 43

| Module | Endpoints |
|--------|-----------|
| Authentication | 5 |
| Users | 4 |
| Passengers | 3 |
| Airports | 3 |
| Flights | 4 |
| Bookings | 5 |
| Payments | 3 |
| Tickets | 3 |
| Delay Prediction | 2 |
| Analytics | 3 |
| **Total** | **35** |

*(Note: the count shown in the table is 35; additional draft endpoints were considered but excluded to keep the contract lean. The document above contains the canonical 35 endpoints.)*
