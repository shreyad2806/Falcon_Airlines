# Falcon Airlines Enterprise

Production-grade Airline Reservation Platform — **Phase 1 backend foundation**.

## Status

Phase 1 is complete: a runnable Spring Boot 3.x backend with Java 21, PostgreSQL, Flyway, Docker, Testcontainers, Swagger and GitHub Actions.

Business modules (auth, booking, payment, ticketing) are planned for Phase 2.

## Quick Start

```bash
cd Falcon-Airlines/falcon-airlines-enterprise/docker
cp .env.example .env
docker-compose up -d postgres

cd ../backend
mvn clean install
mvn spring-boot:run -Dspring.profiles.active=dev
```

Verify the foundation:

- Health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html

## Run Everything in Docker

```bash
cd Falcon-Airlines/falcon-airlines-enterprise/docker
docker-compose up -d
```

## Structure & Setup

- `docs/setup/SETUP.md` — detailed setup and test commands
- `docs/design/PROJECT_STRUCTURE.md` — repository layout
- `docs/learning/` — focused technology notes
