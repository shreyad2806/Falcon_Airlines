# Project Structure

```
falcon-airlines-enterprise/
├── backend/
│   ├── src/main/java/com/falcon/airlines/
│   │   ├── FalconAirlinesApplication.java
│   │   ├── common/              # BaseEntity, AuditEntity
│   │   ├── config/              # OpenApiConfig
│   │   ├── constant/            # ApplicationConstants
│   │   ├── dto/                 # Request/Response DTOs (Phase 2 core)
│   │   ├── entity/              # JPA entities (17 tables)
│   │   ├── enums/               # Domain enumerations
│   │   ├── exception/           # BaseException, GlobalExceptionHandler
│   │   ├── mapper/              # MapStruct mappers
│   │   ├── repository/          # Spring Data JPA repositories
│   │   ├── response/            # ApiResponse, ApiErrorResponse
│   │   └── util/                # DateTimeUtil, StringUtil
│   ├── src/main/resources/
│   │   ├── application*.yml
│   │   ├── banner.txt
│   │   ├── logback-spring.xml
│   │   └── db/migration/
│   │       ├── V1__baseline.sql
│   │       ├── V2__create_schema.sql
│   │       └── V3__seed_reference_data.sql
│   └── src/test/java/com/falcon/airlines/
│       ├── FalconAirlinesApplicationTest.java
│       ├── common/
│       │   ├── BaseIntegrationTest.java
│       │   └── BaseUnitTest.java
│       ├── config/
│       │   └── TestcontainersConfig.java
│       └── repository/
│           └── DatabaseLayerIntegrationTest.java
├── docker/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── .env.example
├── docs/
│   ├── design/
│   ├── learning/
│   └── setup/
├── frontend/                    # Phase 3
└── python-ai/                   # Phase 4
```

> Phase 1 provided the engineering foundation. Phase 2 added the database layer (Flyway schema, 17 JPA entities, repositories, DTOs/mappers for core domains, seed data, and integration tests). Phase 3 will add authentication and business services.
