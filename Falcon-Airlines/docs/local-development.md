# Falcon Airlines — Local Development Guide

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| npm | 9+ |
| Docker | 24+ (for PostgreSQL) |

## Service Architecture

```
Frontend (Vite/React)        →  Spring Boot Backend  →  PostgreSQL
http://localhost:5173            http://localhost:8080     localhost:5433
```

## Startup Order

### 1. Start PostgreSQL

```bash
cd Falcon-Airlines/falcon-airlines-enterprise/docker
docker compose up -d postgres
```

Wait for healthy status:

```bash
docker compose ps
```

### 2. Start Backend

```bash
cd Falcon-Airlines/falcon-airlines-enterprise/backend
mvn spring-boot:run
```

Verify:

```bash
curl http://localhost:8080/actuator/health
```

### 3. Start Frontend

```bash
cd Falcon-Airlines/falcon-airlines-enterprise/frontend
npm install
npm run dev
```

Opens at: **http://localhost:5173**

## URLs

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Health Check | http://localhost:8080/actuator/health |

## Environment Variables

### Backend (optional, all have defaults)

| Variable | Default |
|----------|---------|
| `SERVER_PORT` | 8080 |
| `SPRING_PROFILES_ACTIVE` | dev |
| `SPRING_DATASOURCE_URL` | jdbc:postgresql://localhost:5433/falcon_airlines |
| `SPRING_DATASOURCE_USERNAME` | postgres |
| `SPRING_DATASOURCE_PASSWORD` | postgres |
| `JWT_SECRET` | FalconAirlinesDefaultSecretKeyForJwtHmacAtLeast32Chars |

### Frontend

| Variable | Default |
|----------|---------|
| `VITE_API_BASE_URL` | http://localhost:8080 |

## First-Time Setup

1. Start PostgreSQL and backend
2. Open Swagger UI at http://localhost:8080/swagger-ui/index.html
3. Register a user via `POST /auth/register`:
   ```json
   {
     "username": "admin",
     "email": "admin@falcon.com",
     "password": "Admin123!"
   }
   ```
4. Or use the frontend at http://localhost:5173/register

**Note:** New users are registered as CUSTOMER role. To access admin features (flight/airport management), the user must be assigned ADMIN or AGENT role directly in the database.

## Frontend Features

| Feature | Page | Backend Endpoint |
|---------|------|-----------------|
| Login | /login | POST /auth/login |
| Register | /register | POST /auth/register |
| Flight Search | /flights | GET /api/flights |
| Create Booking | /bookings | POST /api/bookings |
| View Booking | /bookings | GET /api/bookings/reference/{ref} |
| Cancel Booking | /bookings | POST /api/bookings/{id}/cancel |
| View Ticket | /tickets | GET /api/tickets/{id} |
| Download Ticket PDF | /tickets | GET /api/tickets/{id}/pdf |
| Generate Boarding Pass | /boarding-passes | POST /api/boarding-passes/ticket/{ticketId} |
| View Boarding Pass | /boarding-passes | GET /api/boarding-passes/{id} |
| Show QR Code | /boarding-passes | GET /api/boarding-passes/{id}/qr-code |
| Download Boarding Pass PDF | /boarding-passes | GET /api/boarding-passes/{id}/pdf |
| Check In | /boarding-passes | POST /api/boarding-passes/{id}/check-in |
| Board | /boarding-passes | POST /api/boarding-passes/{id}/board |

## Known Limitations

1. **Role assignment** — New users register as CUSTOMER. Flight creation, airport management, and other write operations require ADMIN or AGENT authority. These must be assigned via database.
2. **Data seeding** — No demo data is pre-seeded. Airports, aircraft, and flights must be created via Swagger or the API before booking flows work.
3. **Pagination** — Flight and passenger lists use server-side pagination. "Next" button loads subsequent pages.

## Troubleshooting

- **CORS errors**: Verify `falcon.cors.allowed-origins` includes `http://localhost:5173`
- **403 on login**: Check username/email and password (min 8 chars, upper+lower+digit)
- **Database connection**: Ensure Docker PostgreSQL is running on port 5433
- **Flyway errors**: The dev profile runs migrations automatically on startup
