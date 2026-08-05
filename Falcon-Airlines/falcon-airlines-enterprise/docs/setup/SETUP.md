# Setup Guide

## Prerequisites

- JDK 21 (Temurin recommended)
- Maven 3.9+
- Docker and Docker Compose
- Git

## Local Development

### 1. Start PostgreSQL

```bash
cd Falcon-Airlines/falcon-airlines-enterprise/docker
cp .env.example .env
docker-compose up -d postgres
```

### 2. Run the Backend

```bash
cd Falcon-Airlines/falcon-airlines-enterprise/backend
mvn clean install
mvn spring-boot:run -Dspring.profiles.active=dev
```

### 3. Verify

- Health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- API base: http://localhost:8080/api/v1

### 4. Run Tests

```bash
cd Falcon-Airlines/falcon-airlines-enterprise/backend
mvn test
```

## Full Docker Stack

```bash
cd Falcon-Airlines/falcon-airlines-enterprise/docker
docker-compose up -d
```

The backend waits for PostgreSQL to pass its health check before starting.
