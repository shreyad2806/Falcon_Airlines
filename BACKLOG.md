# Falcon Airlines Enterprise — Implementation Backlog

> **Audience**: Solo developer building the platform over ~10 weeks.  
> **Estimates**: T-shirt sizes — **XS** (<2h), **S** (½ day), **M** (1 day), **L** (2–3 days), **XL** (4–5 days).

---

## How to Read This Backlog

Each **Task** contains:

- **Acceptance Criteria** (Definition of Done)
- **Priority** (High / Medium / Low)
- **Dependencies** (prerequisite tasks)
- **Estimate** (XS, S, M, L, XL)

The backlog is organized into **Milestones**, each with **Epics**, **Features**, and **User Stories**. Tasks are the lowest-level work items.

---

## Milestone 1 — Project Foundation  
*Goal: A runnable, containerized skeleton with database, backend, and frontend.*  
*Target: Week 1–2*

### Epic 1.1 — Repository & Tooling

**Feature 1.1.1 — Monorepo Setup**

- **User Story**: As a developer, I want a single repository so that the backend, frontend, and infrastructure live together.
- **Task 1.1.1.1**: Initialize Git repository with branching strategy and `.gitignore`.
  - **Acceptance Criteria**: `git status` is clean; `main`, `develop`, and `feature/*` branches exist.
  - **Priority**: High
  - **Dependencies**: None
  - **Estimate**: XS
- **Task 1.1.1.2**: Create root `README.md` and `docs/` folder linking PRD, architecture, and module design.
  - **Acceptance Criteria**: README lists tech stack, run instructions, and links all design docs.
  - **Priority**: Medium
  - **Dependencies**: 1.1.1.1
  - **Estimate**: XS

**Feature 1.1.2 — Docker Compose Environment**

- **User Story**: As a developer, I want `docker-compose up` to start the full stack so that I can develop locally.
- **Task 1.1.2.1**: Create `docker-compose.yml` with PostgreSQL, backend, and frontend services.
  - **Acceptance Criteria**: `docker-compose up` starts all services and frontend loads on `localhost:3000`.
  - **Priority**: High
  - **Dependencies**: 1.1.1.1
  - **Estimate**: M
- **Task 1.1.2.2**: Add multi-stage `Dockerfile` for the Spring Boot backend.
  - **Acceptance Criteria**: Image builds in under 5 minutes and passes health check.
  - **Priority**: High
  - **Dependencies**: 1.1.2.1
  - **Estimate**: S
- **Task 1.1.2.3**: Add `Dockerfile` and `nginx` config for the React frontend.
  - **Acceptance Criteria**: Production build runs in a container and serves static files.
  - **Priority**: Medium
  - **Dependencies**: 1.1.2.1
  - **Estimate**: S

### Epic 1.2 — Database Foundation

**Feature 1.2.1 — PostgreSQL Schema**

- **User Story**: As a developer, I want the database schema created automatically so that the application has a data store.
- **Task 1.2.1.1**: Add Flyway/Liquibase migration scripts for core tables (`users`, `roles`, `permissions`, `airports`, `aircraft`, `flights`, `passengers`, `bookings`, `tickets`, `payments`, `notifications`, `audit_logs`).
  - **Acceptance Criteria**: Migrations run successfully on container start; all tables from `DATABASE_DESIGN.md` exist.
  - **Priority**: High
  - **Dependencies**: 1.1.2.1
  - **Estimate**: L
- **Task 1.2.1.2**: Create seed data for airports, aircraft, and sample flights.
  - **Acceptance Criteria**: At least 3 airports, 2 aircraft, and 5 sample flights are inserted.
  - **Priority**: Medium
  - **Dependencies**: 1.2.1.1
  - **Estimate**: S

### Epic 1.3 — Backend Skeleton

**Feature 1.3.1 — Spring Boot Bootstrap**

- **User Story**: As a developer, I want a working Spring Boot project so that I can add business modules.
- **Task 1.3.1.1**: Create Spring Boot multi-module project with `pom.xml` for `authentication`, `booking`, `flight`, `payment`, etc.
  - **Acceptance Criteria**: `mvn clean install` passes; modules compile.
  - **Priority**: High
  - **Dependencies**: 1.1.1.1
  - **Estimate**: M
- **Task 1.3.1.2**: Add shared `common` module with audit columns, soft delete helpers, and `GlobalExceptionHandler`.
  - **Acceptance Criteria**: All modules can import common classes and exceptions return JSON.
  - **Priority**: High
  - **Dependencies**: 1.3.1.1
  - **Estimate**: M
- **Task 1.3.1.3**: Configure Swagger/OpenAPI for API documentation.
  - **Acceptance Criteria**: Swagger UI is reachable at `/swagger-ui.html` and lists at least one endpoint.
  - **Priority**: Medium
  - **Dependencies**: 1.3.1.1
  - **Estimate**: S

### Epic 1.4 — Frontend Skeleton

**Feature 1.4.1 — React Application**

- **User Story**: As a customer, I want a web app so that I can interact with the platform.
- **Task 1.4.1.1**: Initialize React with Vite or CRA, routing, and Material UI / Tailwind.
  - **Acceptance Criteria**: `npm run dev` opens the home page; navigation routes work.
  - **Priority**: High
  - **Dependencies**: 1.1.2.1
  - **Estimate**: S
- **Task 1.4.1.2**: Create a shared API client with base URL and Axios interceptors.
  - **Acceptance Criteria**: The client can make a request to the backend health endpoint.
  - **Priority**: Medium
  - **Dependencies**: 1.4.1.1
  - **Estimate**: S

---

## Milestone 2 — Authentication & User Management  
*Goal: Secure login, registration, JWT, and role-based access.*  
*Target: Week 2–3*

### Epic 2.1 — Authentication

**Feature 2.1.1 — Registration & Login**

- **User Story**: As a customer, I want to register and log in so that I can access my bookings securely.
- **Task 2.1.1.1**: Implement `POST /api/v1/auth/register`.
  - **Acceptance Criteria**: 201 returned on valid input; duplicate email returns 409; weak password returns 422.
  - **Priority**: High
  - **Dependencies**: 1.2.1.1, 1.3.1.2
  - **Estimate**: M
- **Task 2.1.1.2**: Implement `POST /api/v1/auth/login`.
  - **Acceptance Criteria**: 200 returns access and refresh tokens; invalid credentials return 401.
  - **Priority**: High
  - **Dependencies**: 2.1.1.1
  - **Estimate**: M
- **Task 2.1.1.3**: Implement `POST /api/v1/auth/refresh` and `POST /api/v1/auth/logout`.
  - **Acceptance Criteria**: New access token is issued with refresh token; logout revokes refresh token.
  - **Priority**: Medium
  - **Dependencies**: 2.1.1.2
  - **Estimate**: S

**Feature 2.1.2 — JWT Security**

- **User Story**: As the system, I want JWT-based security so that stateless authentication works across services.
- **Task 2.1.2.1**: Configure Spring Security with JWT filter and `Authorization: Bearer` header support.
  - **Acceptance Criteria**: Protected endpoints reject missing/invalid tokens; valid tokens allow access.
  - **Priority**: High
  - **Dependencies**: 2.1.1.2
  - **Estimate**: M
- **Task 2.1.2.2**: Add `users/me` and `PUT users/me` endpoints.
  - **Acceptance Criteria**: Authenticated user can view and update own profile.
  - **Priority**: Medium
  - **Dependencies**: 2.1.2.1
  - **Estimate**: S

### Epic 2.2 — Role Management

**Feature 2.2.1 — Roles & Permissions**

- **User Story**: As an admin, I want to assign roles so that users have correct access.
- **Task 2.2.1.1**: Seed `roles` and `permissions` and create `user_roles` / `role_permissions` mappings.
  - **Acceptance Criteria**: `CUSTOMER`, `AGENT`, `ADMIN` roles exist with correct permissions.
  - **Priority**: High
  - **Dependencies**: 1.2.1.1
  - **Estimate**: S
- **Task 2.2.1.2**: Implement role-based authorization on all protected endpoints.
  - **Acceptance Criteria**: `@PreAuthorize` or filter rejects requests when role is missing; admin-only endpoints are protected.
  - **Priority**: High
  - **Dependencies**: 2.1.2.1, 2.2.1.1
  - **Estimate**: M

---

## Milestone 3 — Master Data  
*Goal: Reference data for airports, aircraft, and passengers.*  
*Target: Week 3–4*

### Epic 3.1 — Airports

**Feature 3.1.1 — Airport CRUD**

- **User Story**: As an admin, I want to manage airports so that flight schedules reference correct locations.
- **Task 3.1.1.1**: Implement `GET /api/v1/airports` and `GET /api/v1/airports/{id}`.
  - **Acceptance Criteria**: List and detail endpoints return airports with IATA/ICAO, timezone, and coordinates.
  - **Priority**: High
  - **Dependencies**: 1.3.1.2, 2.1.2.1
  - **Estimate**: S
- **Task 3.1.1.2**: Implement `POST /api/v1/airports` and `PUT /api/v1/airports/{id}` (admin only).
  - **Acceptance Criteria**: Valid data creates/updates airport; duplicate IATA returns 409.
  - **Priority**: Medium
  - **Dependencies**: 3.1.1.1
  - **Estimate**: S

### Epic 3.2 — Aircraft

**Feature 3.2.1 — Aircraft CRUD**

- **User Story**: As an admin, I want to manage aircraft so that flights are assigned to real fleets.
- **Task 3.2.1.1**: Implement `GET /api/v1/aircraft` and `POST /api/v1/aircraft`.
  - **Acceptance Criteria**: CRUD operations work; duplicate registration numbers rejected.
  - **Priority**: Medium
  - **Dependencies**: 2.1.2.1
  - **Estimate**: S
- **Task 3.2.1.2**: Add seat map and capacity to aircraft records.
  - **Acceptance Criteria**: Aircraft has `configuration` JSON with rows, seats, and cabin layout.
  - **Priority**: Medium
  - **Dependencies**: 3.2.1.1
  - **Estimate**: S

### Epic 3.3 — Passengers

**Feature 3.3.1 — Passenger CRUD**

- **User Story**: As a customer, I want to save passenger details so that booking is faster.
- **Task 3.3.1.1**: Implement `POST /api/v1/passengers` and `GET /api/v1/passengers/{id}`.
  - **Acceptance Criteria**: Passengers are linked to the authenticated user; invalid DOB or nationality rejected.
  - **Priority**: High
  - **Dependencies**: 2.1.2.1, 1.2.1.1
  - **Estimate**: M
- **Task 3.3.1.2**: Implement `PUT /api/v1/passengers/{id}` and duplicate detection.
  - **Acceptance Criteria**: Owner can update passenger; fuzzy name+DOB duplicate warnings shown.
  - **Priority**: Medium
  - **Dependencies**: 3.3.1.1
  - **Estimate**: S

---

## Milestone 4 — Flight Management & Search  
*Goal: Schedule flights and let users search and view inventory.*  
*Target: Week 4–5*

### Epic 4.1 — Flight Schedule Management

**Feature 4.1.1 — Flight CRUD**

- **User Story**: As an admin, I want to schedule flights so that customers can book them.
- **Task 4.1.1.1**: Implement `POST /api/v1/flights`.
  - **Acceptance Criteria**: Flight is created with origin, destination, aircraft, and scheduled times; arrival must be after departure.
  - **Priority**: High
  - **Dependencies**: 3.1.1.1, 3.2.1.1
  - **Estimate**: M
- **Task 4.1.1.2**: Implement `GET /api/v1/flights/{id}` and `PUT /api/v1/flights/{id}`.
  - **Acceptance Criteria**: Detail and update endpoints work; past flights cannot be modified.
  - **Priority**: Medium
  - **Dependencies**: 4.1.1.1
  - **Estimate**: S

### Epic 4.2 — Inventory & Fares

**Feature 4.2.1 — Fare & Inventory Setup**

- **User Story**: As an admin, I want to set fares and inventory so that flights can be sold.
- **Task 4.2.1.1**: Add fare basis, fare families, and inventory classes to flights.
  - **Acceptance Criteria**: Flight record includes `Y`, `M`, `J` inventory counts and fare amounts.
  - **Priority**: High
  - **Dependencies**: 4.1.1.1
  - **Estimate**: M
- **Task 4.2.1.2**: Implement inventory validation before booking.
  - **Acceptance Criteria**: Booking is rejected if requested class has no available seats.
  - **Priority**: High
  - **Dependencies**: 4.2.1.1
  - **Estimate**: S

### Epic 4.3 — Flight Search

**Feature 4.3.1 — Search Endpoint**

- **User Story**: As a customer, I want to search flights so that I can compare options.
- **Task 4.3.1.1**: Implement `GET /api/v1/flights?origin=...&destination=...&departureDate=...`.
  - **Acceptance Criteria**: Results include flight number, departure/arrival, fares, and available cabins.
  - **Priority**: High
  - **Dependencies**: 4.2.1.1
  - **Estimate**: M
- **Task 4.3.1.2**: Add Redis or in-memory cache for search results.
  - **Acceptance Criteria**: Repeated identical search returns faster and does not hit DB each time.
  - **Priority**: Medium
  - **Dependencies**: 4.3.1.1
  - **Estimate**: S

---

## Milestone 5 — Booking Engine & Seat Management  
*Goal: Create, modify, and hold bookings with seat selection.*  
*Target: Week 5–6*

### Epic 5.1 — Booking Lifecycle

**Feature 5.1.1 — Booking Creation**

- **User Story**: As a customer, I want to book a flight so that I can reserve seats.
- **Task 5.1.1.1**: Implement `POST /api/v1/bookings`.
  - **Acceptance Criteria**: Booking holds inventory, generates PNR, returns reference, and sets time limit.
  - **Priority**: High
  - **Dependencies**: 4.2.1.2, 3.3.1.1
  - **Estimate**: L
- **Task 5.1.1.2**: Implement `GET /api/v1/bookings/{reference}`.
  - **Acceptance Criteria**: Booking with passengers, flights, and status is returned; owner/agent only.
  - **Priority**: High
  - **Dependencies**: 5.1.1.1
  - **Estimate**: S

**Feature 5.1.2 — Booking Modifications**

- **User Story**: As a customer, I want to cancel or modify my booking so that I can handle plan changes.
- **Task 5.1.2.1**: Implement `DELETE /api/v1/bookings/{reference}` (cancel).
  - **Acceptance Criteria**: Booking status changes to `CANCELLED` and inventory is released.
  - **Priority**: Medium
  - **Dependencies**: 5.1.1.1
  - **Estimate**: M
- **Task 5.1.2.2**: Implement agent rebooking `POST /api/v1/bookings/{reference}/rebook`.
  - **Acceptance Criteria**: New flights are assigned to the same passengers; old inventory released.
  - **Priority**: Low
  - **Dependencies**: 5.1.1.1
  - **Estimate**: M

### Epic 5.2 — Seat Management

**Feature 5.2.1 — Seat Selection**

- **User Story**: As a customer, I want to choose my seat so that I can sit where I prefer.
- **Task 5.2.1.1**: Implement `POST /api/v1/bookings/{reference}/seats`.
  - **Acceptance Criteria**: Seat is reserved for the passenger; double-booking of the same seat is prevented.
  - **Priority**: Medium
  - **Dependencies**: 3.2.1.2, 5.1.1.1
  - **Estimate**: M
- **Task 5.2.1.2**: Expose seat map endpoint for a flight.
  - **Acceptance Criteria**: `GET /api/v1/flights/{id}/seats` returns rows, columns, and occupied seats.
  - **Priority**: Medium
  - **Dependencies**: 3.2.1.2, 5.2.1.1
  - **Estimate**: S

---

## Milestone 6 — Payments & Ticketing  
*Goal: Accept payments and issue tickets.*  
*Target: Week 6–7*

### Epic 6.1 — Payments

**Feature 6.1.1 — Payment Processing**

- **User Story**: As a customer, I want to pay for my booking so that my reservation is confirmed.
- **Task 6.1.1.1**: Implement `POST /api/v1/payments` with tokenization.
  - **Acceptance Criteria**: Payment is authorized; booking status moves to `CONFIRMED`; PAN is never stored.
  - **Priority**: High
  - **Dependencies**: 5.1.1.1
  - **Estimate**: L
- **Task 6.1.1.2**: Implement `GET /api/v1/payments/{id}`.
  - **Acceptance Criteria**: Payment status and amount returned; owner/agent only.
  - **Priority**: Medium
  - **Dependencies**: 6.1.1.1
  - **Estimate**: S
- **Task 6.1.1.3**: Implement `POST /api/v1/payments/{id}/refunds`.
  - **Acceptance Criteria**: Refund request created and status updated; amount cannot exceed original.
  - **Priority**: Medium
  - **Dependencies**: 6.1.1.1
  - **Estimate**: M

### Epic 6.2 — Ticketing

**Feature 6.2.1 — Ticket Issuance**

- **User Story**: As a customer, I want to receive my e-ticket so that I can board the flight.
- **Task 6.2.1.1**: Implement `GET /api/v1/tickets/{ticketNumber}`.
  - **Acceptance Criteria**: Ticket with passenger, flight, fare, and taxes returned.
  - **Priority**: High
  - **Dependencies**: 6.1.1.1
  - **Estimate**: S
- **Task 6.2.1.2**: Auto-issue ticket on payment success.
  - **Acceptance Criteria**: After payment `CAPTURED`, `tickets` records are created and linked to booking.
  - **Priority**: High
  - **Dependencies**: 6.1.1.1
  - **Estimate**: M
- **Task 6.2.1.3**: Implement `POST /api/v1/tickets/{number}/void` (agent/admin).
  - **Acceptance Criteria**: Ticket status changes to `VOID` within valid window.
  - **Priority**: Low
  - **Dependencies**: 6.2.1.2
  - **Estimate**: S

---

## Milestone 7 — Notifications & Frontend  
*Goal: Inform passengers and deliver usable UI.*  
Target: Week 7–8

### Epic 7.1 — Notifications

**Feature 7.1.1 — Email & SMS Notifications**

- **User Story**: As a customer, I want booking confirmations so that I have proof of purchase.
- **Task 7.1.1.1**: Integrate notification provider (SMTP / Twilio) and create template engine.
  - **Acceptance Criteria**: Emails and SMS can be sent from the application.
  - **Priority**: Medium
  - **Dependencies**: 1.1.2.1
  - **Estimate**: M
- **Task 7.1.1.2**: Trigger notifications on booking, payment, and ticket events.
  - **Acceptance Criteria**: Customer receives confirmation email/SMS after booking and after payment.
  - **Priority**: Medium
  - **Dependencies**: 7.1.1.1, 6.2.1.2
  - **Estimate**: S

### Epic 7.2 — React Customer Portal

**Feature 7.2.1 — Customer UI**

- **User Story**: As a customer, I want a web interface so that I can search and book flights.
- **Task 7.2.1.1**: Build login/register pages.
  - **Acceptance Criteria**: Users can register, log in, and persist JWT.
  - **Priority**: High
  - **Dependencies**: 2.1.1.2, 1.4.1.1
  - **Estimate**: M
- **Task 7.2.1.2**: Build flight search and results pages.
  - **Acceptance Criteria**: Search form and results are rendered with fare details.
  - **Priority**: High
  - **Dependencies**: 4.3.1.1, 7.2.1.1
  - **Estimate**: M
- **Task 7.2.1.3**: Build booking and checkout flow.
  - **Acceptance Criteria**: Customer can select passengers, create booking, and see confirmation.
  - **Priority**: High
  - **Dependencies**: 5.1.1.1, 7.2.1.2
  - **Estimate**: L
- **Task 7.2.1.4**: Build “My Trips” and profile pages.
  - **Acceptance Criteria**: Customer can view bookings and update profile.
  - **Priority**: Medium
  - **Dependencies**: 7.2.1.3
  - **Estimate**: M

---

## Milestone 8 — Delay Prediction & Analytics  
*Goal: Add AI-powered disruption forecasts and operational dashboards.*  
*Target: Week 8–9*

### Epic 8.1 — Delay Prediction

**Feature 8.1.1 — Python AI Microservice**

- **User Story**: As operations, I want delay predictions so that I can plan proactively.
- **Task 8.1.1.1**: Create Python FastAPI service with `/predict/{flight_id}` endpoint.
  - **Acceptance Criteria**: Service is containerized and reachable from the Spring Boot backend.
  - **Priority**: Medium
  - **Dependencies**: 1.1.2.1
  - **Estimate**: M
- **Task 8.1.1.2**: Train or mock a delay prediction model with weather/schedule features.
  - **Acceptance Criteria**: Endpoint returns `predicted_delay_minutes`, `probability`, and `risk_level`.
  - **Priority**: Medium
  - **Dependencies**: 8.1.1.1
  - **Estimate**: L
- **Task 8.1.1.3**: Wire Spring Boot `GET /api/v1/delay-predictions/flights/{flightId}` to Python service.
  - **Acceptance Criteria**: Prediction response is exposed through the main API with auth.
  - **Priority**: Medium
  - **Dependencies**: 8.1.1.2, 2.1.2.1
  - **Estimate**: S

### Epic 8.2 — Analytics

**Feature 8.2.1 — Operational Dashboard**

- **User Story**: As an admin, I want dashboards so that I can monitor sales and performance.
- **Task 8.2.1.1**: Implement `GET /api/v1/analytics/dashboard`.
  - **Acceptance Criteria**: Returns total bookings, revenue, load factor, and ancillary revenue for the period.
  - **Priority**: Medium
  - **Dependencies**: 6.2.1.2
  - **Estimate**: M
- **Task 8.2.1.2**: Implement `GET /api/v1/analytics/reports/sales` and `/reports/flight-performance`.
  - **Acceptance Criteria**: Reports support filters and return CSV/JSON.
  - **Priority**: Low
  - **Dependencies**: 8.2.1.1
  - **Estimate**: M
- **Task 8.2.1.3**: Create a React admin dashboard.
  - **Acceptance Criteria**: Admin can view KPI cards and charts.
  - **Priority**: Low
  - **Dependencies**: 8.2.1.1, 2.2.1.2
  - **Estimate**: L

---

## Milestone 9 — Final Integration & Release  
*Goal: Polish, test, secure, and deploy a demonstrable product.*  
*Target: Week 9–10*

### Epic 9.1 — Testing

**Feature 9.1.1 — Quality Assurance**

- **User Story**: As a developer, I want automated tests so that the system is reliable.
- **Task 9.1.1.1**: Write unit and integration tests for authentication and booking flows.
  - **Acceptance Criteria**: `mvn test` and `npm test` pass with >70% coverage on critical paths.
  - **Priority**: High
  - **Dependencies**: 5.1.1.1
  - **Estimate**: L
- **Task 9.1.1.2**: Add end-to-end tests for search-book-pay journey.
  - **Acceptance Criteria**: E2E test creates a booking and verifies ticket creation.
  - **Priority**: Medium
  - **Dependencies**: 9.1.1.1
  - **Estimate**: M

### Epic 9.2 — Security & Hardening

**Feature 9.2.1 — Security Review**

- **User Story**: As a platform, I want to be secure so that customer data is protected.
- **Task 9.2.1.1**: Run OWASP dependency and secret scans.
  - **Acceptance Criteria**: No critical vulnerabilities; no secrets in repository.
  - **Priority**: High
  - **Dependencies**: 1.1.1.1
  - **Estimate**: S
- **Task 9.2.1.2**: Add rate limiting, CORS, and input sanitization.
  - **Acceptance Criteria**: Login endpoints are rate-limited; CORS allows only frontend origin.
  - **Priority**: High
  - **Dependencies**: 2.1.2.1
  - **Estimate**: S

### Epic 9.3 — Deployment & Documentation

**Feature 9.3.1 — Production Deployment**

- **User Story**: As a developer, I want a demo deployment so that the project can be shared.
- **Task 9.3.1.1**: Add `docker-compose.prod.yml` and environment-specific configs.
  - **Acceptance Criteria**: Production compose starts the full stack with a single command.
  - **Priority**: Medium
  - **Dependencies**: 1.1.2.1
  - **Estimate**: M
- **Task 9.3.1.2**: Update README with setup, architecture, and run instructions.
  - **Acceptance Criteria**: New user can clone, run, and navigate the app in under 15 minutes.
  - **Priority**: Medium
  - **Dependencies**: 9.3.1.1
  - **Estimate**: S
- **Task 9.3.1.3**: Record a short demo or Loom video.
  - **Acceptance Criteria**: Video shows search, booking, payment, and ticket view.
  - **Priority**: Low
  - **Dependencies**: 9.3.1.2
  - **Estimate**: S

---

## Summary by Milestone

| Milestone | Focus | Approx. Duration | Key Deliverable |
|-----------|-------|------------------|-----------------|
| M1 | Foundation | Week 1–2 | `docker-compose up` with skeleton backend, frontend, and DB |
| M2 | Auth & Users | Week 2–3 | JWT login/register and role-based access |
| M3 | Master Data | Week 3–4 | Airports, aircraft, and passenger CRUD |
| M4 | Flights & Search | Week 4–5 | Scheduled flights and search results |
| M5 | Booking & Seats | Week 5–6 | PNR creation, hold, cancel, seat selection |
| M6 | Payments & Ticketing | Week 6–7 | Payment authorization and e-ticket issuance |
| M7 | Notifications & UI | Week 7–8 | Customer portal and confirmations |
| M8 | AI & Analytics | Week 8–9 | Delay prediction and admin dashboards |
| M9 | Final Release | Week 9–10 | Tests, security, and deployed demo |

## Estimate Distribution

| Size | Approx. Effort | Count |
|------|----------------|-------|
| **XS** | < 2 hours | 4 |
| **S** | ½ day | 24 |
| **M** | 1 day | 18 |
| **L** | 2–3 days | 8 |
| **XL** | 4–5 days | 0 |
