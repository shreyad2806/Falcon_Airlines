# Architecture Overview

## Layers

| Layer | Responsibility |
|-------|----------------|
| **Controller** | HTTP entry point, DTO mapping, validation |
| **Service** | Business logic and transaction coordination |
| **Repository** | Data access via Spring Data JPA / JDBC |
| **Database** | PostgreSQL with Flyway migrations |

## Technology

- Java 21 and Spring Boot 3.x
- Spring Data JPA with Hibernate
- PostgreSQL
- Docker and Docker Compose
- Spring Security (Phase 2)
- JWT (Phase 2)

## Security

- Phase 1 uses a permissive Spring Security config so the foundation can start.
- JWT, RBAC, and MFA are reserved for Phase 2.

## Packaging

The monorepo keeps the backend, frontend, AI service, and database in one repository for atomic changes and consistent versioning.
