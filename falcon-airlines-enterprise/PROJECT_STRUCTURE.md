# Project Structure

```
falcon-airlines-enterprise/
├── backend/
│   ├── src/main/java/com/falcon/airlines/
│   │   ├── controller/      # REST controllers
│   │   ├── service/         # Business logic
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── entity/          # JPA entities
│   │   ├── dto/             # Data transfer objects
│   │   ├── mapper/          # MapStruct mappers
│   │   ├── security/        # JWT and RBAC (Phase 2)
│   │   ├── config/          # Spring and OpenAPI config
│   │   ├── validation/      # Custom validators
│   │   ├── exception/       # Global exception handling
│   │   ├── util/            # Utility classes
│   │   ├── scheduler/       # Scheduled jobs
│   │   ├── integration/     # External service clients
│   │   ├── constant/        # Application constants
│   │   ├── common/          # Shared base classes
│   │   ├── audit/           # Audit helpers
│   │   ├── event/           # Domain events
│   │   ├── logging/         # Logging utilities
│   │   ├── health/          # Custom health checks
│   │   ├── response/        # Standard response wrappers
│   │   ├── request/         # Request DTOs
│   │   └── enums/           # Enumerations
│   ├── src/main/resources/
│   │   ├── db/migration/    # Flyway SQL migrations
│   │   ├── static/          # Static assets
│   │   ├── templates/       # Email templates
│   │   └── application*.yml # Spring profiles
│   └── pom.xml
├── frontend/                # React (Phase 2)
├── python-ai/               # FastAPI ML (Phase 3)
├── database/
│   └── migrations/          # Reference migrations
├── docker/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── .env.example
├── docs/                    # Design documents
├── learning/                # 20 learning notes
├── .github/
│   └── workflows/           # CI/CD
└── scripts/                 # Automation scripts
```
