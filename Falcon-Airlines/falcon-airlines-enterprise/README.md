# Falcon Airlines Enterprise

A production-grade Airline Reservation Platform built with Java 21, Spring Boot 3.x, PostgreSQL, React, and Docker.

## Status

**Phase 1 — Project Foundation** (in progress).  
This repository currently contains the monorepo skeleton, Spring Boot backend foundation, Docker environment, CI/CD scaffolding, and learning documentation. Business modules (auth, booking, payment, ticketing) are planned for later phases.

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA, Hibernate, JDBC
- **Database**: PostgreSQL 16, Flyway
- **Frontend**: React (planned)
- **AI/ML**: Python FastAPI (planned)
- **DevOps**: Docker, Docker Compose, GitHub Actions
- **API Docs**: OpenAPI / SpringDoc
- **Testing**: JUnit 5, Mockito, Testcontainers

## Repository Structure

```
falcon-airlines-enterprise/
├── backend/        # Spring Boot application
├── frontend/       # React application
├── python-ai/      # Machine learning microservices
├── database/       # Migrations and seeds
├── docker/         # Docker and compose files
├── docs/           # Architecture and design docs
├── learning/       # Interview and learning notes
├── .github/        # GitHub Actions workflows
└── scripts/        # Automation scripts
```

## Quick Start

See [SETUP.md](./SETUP.md) for environment setup.

```bash
cd docker
docker-compose up -d
```

## Documentation

- [PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md)
- [ARCHITECTURE_OVERVIEW.md](./ARCHITECTURE_OVERVIEW.md)
- [ROADMAP.md](./ROADMAP.md)
- [CONTRIBUTING.md](./CONTRIBUTING.md)
- [learning/](./learning/)
