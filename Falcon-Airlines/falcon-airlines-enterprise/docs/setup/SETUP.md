# Setup Guide

## Prerequisites

- JDK 21 (Temurin recommended)
- Maven 3.9+
- Docker and Docker Compose
- Node.js 20+ (future frontend)
- Git

## Local Development

### 1. Start PostgreSQL

```bash
cd docker
cp .env.example .env
docker-compose up -d postgres
```

### 2. Run the Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. Verify

- API: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator/health

### 4. Run Tests

```bash
cd backend
mvn test
```

## Docker

```bash
cd docker
docker-compose up -d
```

The backend will wait for PostgreSQL to pass its health check before starting.
