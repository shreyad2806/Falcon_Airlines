# Falcon Airlines Enterprise — Complete System Architecture

## 1. High-Level Architecture

```
+-------------------------------------------------------------+
|                         Client Layer                        |
|  +----------+  +----------+  +----------+  +-------------+  |
|  | React    |  | Mobile   |  | Kiosk    |  | Partner/    |  |
|  | Web App  |  | Web View |  | Terminal |  | B2B API     |  |
|  +----+-----+  +-----+----+  +-----+----+  +------+------+  |
|       |              |             |              |         |
+-------|--------------|-------------|--------------|---------+
        |              |             |              |
        +--------------+-------------+--------------+
                       |
              +--------v--------+
              |  CDN / WAF      |   (Static assets, DDoS protection)
              +--------+--------+
                       |
              +--------v--------+
              |  API Gateway    |   (Routing, throttling, JWT validation)
              +--------+--------+
                       |
       +---------------+---------------+---------------+-----------------+
       |               |               |               |                 |
+------v-----+  +------v------+  +-----v-------+  +----v-----+  +-------v-------+
|  Auth      |  |  Booking    |  |  Flight     |  |  Payment |  |  Python AI    |
|  Service   |  |  Service    |  |  Service    |  |  Service |  |  Microservice |
|  (JWT/RBAC)|  |  (DDD Core) |  |  Inventory  |  |  PCI DSS |  |  (Delay/ML)   |
+------+-----+  +------+------+  +------+------+  +----+-----+  +-------+-------+
       |               |               |               |                 |
       +---------------+---------------+---------------+-----------------+
                       |
              +--------v--------+
              |  Event Bus      |   (Asynchronous domain events)
              +--------+--------+
                       |
       +---------------+---------------+-------------------+
       |               |               |                   |
+------v-----+  +------v------+  +-----v-------+  +--------v--------+
| PostgreSQL |  |  Cache      |  |  Object     |  |  Message Queue  |
| Cluster    |  |  (Search    |  |  Storage    |  |  (Notifications)|
| (Main DB)  |  |  results)   |  |  (BP/       |  |                 |
+------------+  +-------------+  |  Boarding   |  +-----------------+
                                 |  passes)    |
                                 +-------------+
```

### Why Each Component Exists

| Component | Purpose |
|-----------|---------|
| **React Web App** | Rich, interactive customer portal for search, booking, check-in, and trip management. |
| **Mobile / Kiosk** | Extends the same platform to on-the-go passengers and airport self-service terminals. |
| **Partner B2B API** | Enables GDS, travel agents, and corporate partners to shop and book programmatically. |
| **CDN / WAF** | Delivers static assets from edge locations and protects the platform from common web attacks. |
| **API Gateway** | Single entry point for all clients; enforces throttling, JWT validation, and service routing. |
| **Auth Service** | Centralizes login, token issuance, MFA, and RBAC/permission decisions. |
| **Booking Service** | Core reservation orchestrator; handles PNR lifecycle, inventory holds, and fare quotes. |
| **Flight Service** | Owns schedules, aircraft, inventory, and fare-product configuration. |
| **Payment Service** | Isolates PCI scope by managing all payment, refund, and reconciliation logic. |
| **Python AI Microservice** | Runs ML models for delay prediction and disruption risk without blocking the Java stack. |
| **PostgreSQL Cluster** | Primary transactional store for all modules; provides ACID guarantees and JSON/relational support. |
| **Cache** | Reduces load on the database by storing search results, fare quotes, and session data. |
| **Object Storage** | Stores boarding passes, invoices, E-tickets, and other immutable documents. |
| **Message Queue** | Decouples heavy or time-sensitive work such as notifications, ticket issuance, and audit logging. |
| **Event Bus** | Propagates domain events (booking confirmed, flight delayed, payment received) across modules. |

---

## 2. Component Diagram

```
+----------------------------------------------------------------------+
|                         Falcon Airlines Enterprise                   |
+----------------------------------------------------------------------+
|                                                                      |
|   +-------------+        +----------------+        +-------------+   |
|   |  React UI   |<------>|  API Gateway   |<------>|  Swagger UI |   |
|   |  (Customer) |        |  (Spring Cloud)|        |  (OpenAPI)  |   |
|   +-------------+        +--------+-------+        +-------------+   |
|                                   |                                  |
|   +-------------------------------+--------------------------------+  |
|   |                         Microservices                        |  |
|   |  +-------+  +--------+  +--------+  +-------+  +---------+  |  |
|   |  | Auth  |  |Booking |  | Flight |  | Seat  |  | Payment |  |  |
|   |  +-------+  +--------+  +--------+  +-------+  +---------+  |  |
|   |  +-------+  +--------+  +--------+  +-------+  +---------+  |  |
|   |  |Ticket |  |Notif.  |  |Analytics|  |Audit  |  | Role    |  |  |
|   |  +-------+  +--------+  +--------+  +-------+  +---------+  |  |
|   |  +------------------------+  +---------------------------+  |  |
|   |  |   Passenger Mgmt       |  |  Python AI (Delay Pred.)  |  |  |
|   |  +------------------------+  +---------------------------+  |  |
|   +-------------------------------------------------------------+  |
|                                                                      |
|   +----------------+   +----------------+   +-------------------+  |
|   |  PostgreSQL    |   |    Cache       |   |   Object Store    |  |
|   |  (Main DB)     |   |  (Redis/EH)    |   | (Boarding Passes) |  |
|   +----------------+   +----------------+   +-------------------+  |
|                                                                      |
|   +----------------+   +----------------+   +-------------------+  |
|   |  Message Queue |   |   Event Bus    |   |  External PSP /   |  |
|   | (Notification) |   |  (Domain evts) |   |  Tax / GDS APIs   |  |
|   +----------------+   +----------------+   +-------------------+  |
|                                                                      |
+----------------------------------------------------------------------+
```

### Component Explanations

| Component | Why It Exists |
|-----------|---------------|
| **API Gateway** | Hides the internal topology, applies cross-cutting concerns (auth, rate limits, logging), and provides a single SSL endpoint. |
| **Auth Service** | Isolates identity so every other service can rely on signed JWTs instead of re-implementing authentication. |
| **Booking Service** | The transactional heart of the system; keeping it independent lets it scale independently during flash sales. |
| **Flight Service** | Separates schedule/inventory complexity from the booking lifecycle, allowing commercial teams to change fares without touching bookings. |
| **Passenger Management** | Centralizes PII, consent, and travel documents so privacy rules are enforced in one place. |
| **Seat Management** | Treats seats as inventory with real-time allocation; keeps the booking engine free of seat-map logic. |
| **Payment Service** | Reduces PCI scope and isolates payment failures from the main booking path. |
| **Ticket Generation** | Owns e-ticket/EMD creation, revalidation, and IATA reporting rules. |
| **Notifications** | Manages templating, localization, and delivery channels without coupling to other domains. |
| **Analytics** | Aggregates operational and commercial data without taxing the transactional database. |
| **Audit Logs** | Provides an immutable, centralized record for compliance and forensic analysis. |
| **Role Management** | Enforces fine-grained permissions across services and avoids ad-hoc authorization logic. |
| **Python AI Microservice** | Offloads heavy ML inference from the Java services and lets data scientists iterate independently. |
| **Swagger UI** | Exposes OpenAPI contracts so frontend, partners, and QA can discover and test endpoints. |

---

## 3. Layered Architecture (Inside a Spring Boot Service)

```
+-------------------------------------------------------------+
|                         Controller Layer                    |
|  +-----------------+  +-----------------+  +----------------+ |
|  | REST Controller |  | DTO / Request   |  | Validation    | |
|  | (HTTP In/Out)   |  | Mappers         |  | Annotations   | |
|  +-----------------+  +-----------------+  +----------------+ |
+-------------------------------------------------------------+
|                         Service Layer                       |
|  +-----------------+  +-----------------+  +----------------+ |
|  | Domain Service  |  | Business Rules  |  | Saga / Tx     | |
|  | Orchestration   |  | Engine          |  | Coordination  | |
|  +-----------------+  +-----------------+  +----------------+ |
+-------------------------------------------------------------+
|                         Repository Layer                    |
|  +-----------------+  +-----------------+  +----------------+ |
|  | Spring Data JPA |  | Hibernate       |  | Custom JDBC   | |
|  | Interfaces      |  | Mappings        |  | Queries       | |
|  +-----------------+  +-----------------+  +----------------+ |
+-------------------------------------------------------------+
|                         Database Layer                      |
|  +-----------------+  +-----------------+  +----------------+ |
|  | PostgreSQL      |  | Domain Tables   |  | Audit / Logs  | |
|  | (ACID)          |  |                 |  |               | |
|  +-----------------+  +-----------------+  +----------------+ |
+-------------------------------------------------------------+
```

### Layer Responsibilities

| Layer | Responsibility | Why It Exists |
|-------|----------------|---------------|
| **Controller** | Accepts HTTP requests, maps DTOs, validates input, and returns responses. | Keeps HTTP concerns out of business logic and provides the Swagger/OpenAPI surface. |
| **Service** | Encapsulates domain logic, enforces invariants, and coordinates transactions/sagas. | This is the business rules layer; it isolates the “what” from the “how” of persistence. |
| **Repository** | Abstracts data access via Spring Data JPA, Hibernate ORM, and targeted JDBC. | Allows the service layer to remain database-agnostic while supporting complex queries. |
| **Database** | Persists aggregates with ACID guarantees and stores structured and semi-structured data. | PostgreSQL supports relational models, JSON, and strong consistency for financial bookings. |

---

## 4. Security Flow

```
+---------+             +----------------+            +----------------+
| Client  |             | API Gateway    |            | Auth Service   |
+----+----+             +--------+-------+            +--------+-------+
     |                          |                              |
     | (1) Request with JWT     |                              |
     +------------------------->|                              |
     |                          | (2) Extract & validate token |
     |                          +----------------------------->|
     |                          |                              |
     |                          | (3) Return claims / valid    |
     |                          |<-----------------------------+
     |                          |                              |
     |                          | (4) Enforce RBAC check       |
     |                          | (5) Route to service         |
     |                          |                              |
     |                          |         +----------------+     |
     |                          |         | Target Service |     |
     |                          +-------->| (scope/role)   |     |
     |                          |         +----------------+     |
     | (6) Response             |                              |
     |<-------------------------+                              |
```

### Why This Flow Exists

| Step | Purpose |
|------|---------|
| **(1) JWT in header** | Carries signed proof of identity and permissions without sending credentials on every call. |
| **(2) API Gateway validation** | Stops invalid or expired tokens before they reach microservices, reducing attack surface. |
| **(3) Auth Service verification** | Centralizes token and JWKS validation, ensuring a single source of truth for token trust. |
| **(4) RBAC enforcement** | Checks whether the caller has the required role/permission for the requested resource. |
| **(5) Microservice routing** | Forwards the request with the validated user context (e.g., userId, roles) so services can make fine-grained decisions. |
| **(6) Secure response** | Data is encrypted in transit and returned only if the caller is authorized. |

---

## 5. Authentication Flow

```
+---------+             +----------------+            +----------------+
| Client  |             | API Gateway    |            | Auth Service   |
+----+----+             +--------+-------+            +--------+-------+
     |                          |                              |
     | (1) POST /login          |                              |
     | (username/password)      |                              |
     +------------------------->|                              |
     |                          | (2) Forward to Auth          |
     |                          +----------------------------->|
     |                          |                              |
     |                          | (3) Validate credentials     |
     |                          |     against PostgreSQL       |
     |                          |                              |
     |                          | (4) MFA challenge if enabled |
     |                          |<-----------------------------+
     |                          |                              |
     | (5) Send / verify MFA    |                              |
     +------------------------->|                              |
     |                          +----------------------------->|
     |                          |                              |
     |                          | (6) Issue JWT + refresh      |
     |                          |     tokens                   |
     |                          |<-----------------------------+
     |                          |                              |
     | (7) Return tokens        |                              |
     |<-------------------------+                              |
     |                          |                              |
     | (8) Call APIs with JWT   |                              |
     +------------------------->+------------------------------>|
```

### Why This Flow Exists

| Step | Purpose |
|------|---------|
| **(1) Login request** | Collects user credentials over TLS. |
| **(2) Forward to Auth** | Keeps authentication logic centralized and avoids duplicating it in every service. |
| **(3) Credential validation** | Compares hashed credentials and enforces account lockout and password policies. |
| **(4) MFA** | Adds a second factor for privileged users (admins, agents) to reduce credential theft risk. |
| **(5) MFA verification** | Validates the one-time code or push approval. |
| **(6) Token issuance** | Creates short-lived access JWT and a longer-lived refresh token. |
| **(7) Token delivery** | Client stores tokens securely and sends the access token with each request. |
| **(8) Authenticated API usage** | Enables stateless, scalable API access with periodic refresh. |

---

## 6. Booking Flow

```
+---------+          +----------------+          +----------------+          +----------------+
| Customer|          | React UI       |          | API Gateway    |          | Booking Service|
+----+----+          +--------+-------+          +--------+-------+          +--------+-------+
     |                        |                          |                          |
     | (1) Search flights     |                          |                          |
     +----------------------->|                          |                          |
     |                        | (2) GET /flights         |                          |
     |                        +------------------------->|                          |
     |                        |                          | (3) Query Flight Service |                          |
     |                        |                          +------------------------->|                          |
     |                        |                          |                          |                          |
     |                        |                          |<-------------------------+                          |
     |                        |<-------------------------+                          |
     | (4) Select flights,    |                          |                          |
     |     passengers, extras |                          |                          |
     +----------------------->|                          |                          |
     |                        | (5) POST /bookings       |                          |
     |                        +------------------------->|                          |
     |                        |                          | (6) Hold inventory       |
     |                        |                          +------------------------->|
     |                        |                          |                          |
     |                        |                          | (7) Reserve seats        |
     |                        |                          |<-------------------------+
     |                        |                          |                          |
     |                        |                          | (8) Calculate fare/tax   |
     |                        |                          |                          |
     | (9) Pay                |                          |                          |
     +----------------------->|                          |                          |
     |                        | (10) POST /payments      |                          |
     |                        |                          | (11) Authorize via PSP   |
     |                        |                          |                          |
     |                        |                          | (12) Payment success     |
     |                        |                          |                          |
     |                        |                          | (13) Issue ticket        |
     |                        |                          |                          |
     |                        |                          | (14) Send confirmation   |
     |                        |                          |                          |
     | (15) Booking confirmed |                          |                          |
     |<-----------------------+                          |                          |
```

### Why This Flow Exists

| Step | Purpose |
|------|---------|
| **(1-3) Search** | Customers discover flights; results are served by the Flight Service with cached inventory. |
| **(4-6) Create booking** | The Booking Service holds inventory and creates a PNR before payment to avoid over-selling. |
| **(7) Seat reservation** | Seat Management locks chosen seats to prevent double assignment. |
| **(8) Fare/tax calculation** | Fare rules, taxes, and ancillaries are computed and persisted for audit. |
| **(9-12) Payment** | Payment Service handles authorization; no ticket is issued until funds are confirmed. |
| **(13) Ticket issuance** | Ticket Generation converts the paid PNR into an e-ticket/EMD. |
| **(14) Confirmation** | Notifications sends itinerary and ticket to the passenger via preferred channel. |
| **(15) Final state** | Customer receives a confirmed booking and e-ticket. |

---

## 7. Delay Prediction Flow

```
+----------------+        +-------------------+        +-------------------+
| Data Sources   |        | Python AI Service |        | Consumers         |
+----------------+        +---------+---------+        +---------+---------+
        |                           |                            |
        | (1) Operational data      |                            |
        | (weather, ATC, crew,      |                            |
        |  aircraft, historical)    |                            |
        +-------------------------->|                            |
        |                           | (2) Preprocess and feature |
        |                           |     engineering            |
        |                           |                            |
        |                           | (3) Run ML inference       |
        |                           |                            |
        |                           | (4) Generate delay score   |
        |                           |     and recommendations    |
        |                           |                            |
        |                           | (5) Publish prediction     |
        |                           |     event                  |
        |                           +--------------------------->|
        |                           |                            |
        |                           |                            | (6) Notify passengers
        |                           |                            | (7) Rebooking options
        |                           |                            | (8) Operations dashboard
```

### Why This Flow Exists

| Step | Purpose |
|------|---------|
| **(1) Data ingestion** | Collects structured and unstructured operational signals needed for prediction. |
| **(2) Feature engineering** | Transforms raw data into model-ready features without impacting core transaction services. |
| **(3) ML inference** | Runs a trained model in a dedicated Python service, independent of the Java stack. |
| **(4) Prediction output** | Produces delay probability, severity, and suggested rebooking flights. |
| **(5) Domain event** | Publishes the prediction so other modules can act on it asynchronously. |
| **(6-8) Consumers** | Notifications, Booking Engine, and Analytics use the event for passenger alerts, rebooking, and planning. |

---

## 8. Deployment Architecture

```
+-------------------------------------------------------------+
|                         Cloud / Data Center                 |
+-------------------------------------------------------------+
|                                                             |
|  +----------------+       +----------------+                |
|  | Load Balancer  |       |  CI/CD Pipeline|                |
|  +--------+-------+       +--------+-------+                |
|           |                        |                        |
|  +--------v-------+                |                        |
|  |  API Gateway   |                |                        |
|  |  Container     |                |                        |
|  +--------+-------+                |                        |
|           |                        |                        |
|  +--------v------------------------v-------+                |
|  |           Docker / Kubernetes Cluster    |                |
|  |  +-------+  +-------+  +-------+  +----+ |                |
|  |  | Auth  |  |Booking|  | Flight|  | ...| |                |
|  |  +-------+  +-------+  +-------+  +----+ |                |
|  |  +----------------+  +------------------+ |                |
|  |  | Python AI Pod  |  | React Static Pod | |                |
|  |  +----------------+  +------------------+ |                |
|  +------------------------------------------+                |
|                          |                                  |
|  +-----------------------v-----------------------+            |
|  |              Persistent Layer                  |            |
|  |  +----------------+ +----------------+        |            |
|  |  | PostgreSQL     | |    Redis       |        |            |
|  |  | (Primary +     | |    Cache       |        |            |
|  |  |  Replicas)     | +----------------+        |            |
|  |  +----------------+                          |            |
|  |  +----------------+ +----------------+        |            |
|  |  | Object Store   | | Message Queue  |        |            |
|  |  | (Documents)    | | (Events)       |        |            |
|  |  +----------------+ +----------------+        |            |
|  +-----------------------------------------------+            |
|                                                             |
+-------------------------------------------------------------+
```

### Why This Deployment Exists

| Component | Purpose |
|-----------|---------|
| **Load Balancer** | Distributes incoming traffic across gateway instances and provides SSL termination. |
| **API Gateway Container** | Runs as a replicated, stateless service to handle routing and token validation. |
| **Docker / Kubernetes Cluster** | Packages and orchestrates each Spring Boot and Python service independently for scaling and resilience. |
| **React Static Pod** | Serves the built React application from a lightweight static server. |
| **PostgreSQL Primary + Replicas** | Provides high availability and read-scaling for transactional and reporting workloads. |
| **Redis Cache** | Offloads read-heavy search and session data from the database. |
| **Object Store** | Keeps large, immutable files (boarding passes, E-tickets, reports) outside the database. |
| **Message Queue** | Buffers asynchronous tasks like notifications, ticket printing, and audit ingestion. |
| **CI/CD Pipeline** | Automates building Docker images, running tests, and deploying canary releases. |

---

## Key Architectural Decisions

| Decision | Rationale |
|----------|-----------|
| **Microservices by domain** | Each DDD bounded context owns its own data and lifecycle, allowing independent scaling and deployment. |
| **Spring Boot + Spring Security + JWT** | Mature, enterprise-ready stack with strong support for secure, stateless REST services. |
| **PostgreSQL as primary store** | ACID transactions, JSON/JSONB flexibility, and strong consistency are critical for booking and payment data. |
| **JPA + Hibernate + targeted JDBC** | JPA accelerates standard CRUD; JDBC is reserved for high-performance, complex reporting or bulk queries. |
| **Python AI microservice** | Keeps ML experimentation, Python libraries, and model lifecycle separate from the Java transactional stack. |
| **Docker + Kubernetes** | Enables portable, consistent deployments, horizontal scaling, and rolling updates in production. |
| **API Gateway as security gate** | Centralizes TLS, throttling, JWT validation, and routing so individual services can focus on business logic. |
