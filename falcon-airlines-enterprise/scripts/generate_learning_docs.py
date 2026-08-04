from pathlib import Path

BASE = Path(r"C:\Users\Shreya Dubey\OneDrive\Documents\Projects\Airline_Managemnt\falcon-airlines-enterprise\learning")

DOCS = [
    ("01_Java21.md", "Java 21", "virtual threads, record patterns, sequenced collections, ZGC generational"),
    ("02_SpringBoot.md", "Spring Boot", "auto-configuration, starter dependencies, embedded Tomcat, Spring Boot 3.x"),
    ("03_Maven.md", "Maven", "dependency management, lifecycle, plugins, multi-module builds"),
    ("04_DependencyInjection.md", "Dependency Injection", "IoC, @Autowired, constructor injection, loose coupling"),
    ("05_BeanLifecycle.md", "Bean Lifecycle", "@PostConstruct, @PreDestroy, InitializingBean, DisposableBean"),
    ("06_ProjectArchitecture.md", "Project Architecture", "layered, hexagonal, DDD, microservices"),
    ("07_SpringAnnotations.md", "Spring Annotations", "@Component, @Service, @Repository, @RestController, @Configuration"),
    ("08_SpringDataJPA.md", "Spring Data JPA", "repositories, query methods, pagination, specifications"),
    ("09_Hibernate.md", "Hibernate", "ORM, first/second level cache, lazy loading, N+1"),
    ("10_JDBC.md", "JDBC", "connections, prepared statements, connection pooling, JdbcTemplate"),
    ("11_PostgreSQL.md", "PostgreSQL", "ACID, indexing, JSONB, partitioning, replication"),
    ("12_Docker.md", "Docker", "containers, images, multi-stage builds, Docker Compose"),
    ("13_Flyway.md", "Flyway", "versioned migrations, repeatable migrations, baseline"),
    ("14_Testing.md", "Testing", "JUnit 5, Mockito, Testcontainers, integration vs unit"),
    ("15_Swagger.md", "Swagger", "OpenAPI, SpringDoc, API contracts, code generation"),
    ("16_Logging.md", "Logging", "SLF4J, Logback, structured logging, log levels"),
    ("17_ExceptionHandling.md", "Exception Handling", "@RestControllerAdvice, global handler, problem details"),
    ("18_SpringSecurity.md", "Spring Security", "filters, JWT, RBAC, OAuth2, password hashing"),
    ("19_InterviewNotes.md", "Interview Notes", "system design, behavioural, coding, resume"),
    ("20_ProductionChecklist.md", "Production Checklist", "SLOs, monitoring, security, backups, DR"),
]

TEMPLATE = """# {title}

## Theory
{theory}.

## How Falcon Airlines Uses It
- Configured in the Spring Boot backend.
- Used to build a scalable airline reservation platform.
- Integrated with PostgreSQL, Docker, and Testcontainers.

## Common Interview Questions
1. What is {title} and why is it important?
2. How does {title} compare to its main alternatives?
3. What are common pitfalls when using {title} in production?

## Best Practices
- Follow official documentation and LTS versions.
- Keep configuration external and environment-specific.
- Write tests before/while using the technology.

## Common Mistakes
- Ignoring security and performance implications.
- Hardcoding secrets in source files.
- Not testing on production-like environments.

## Real Enterprise Example
Large airlines, banks, and e-commerce platforms rely on {title} for mission-critical systems.

## Code Snippet
```java
// {title} example
// Add relevant code here during implementation
```

## References
- https://docs.spring.io/
- https://www.postgresql.org/docs/
- https://openjdk.org/
"""

for filename, title, theory in DOCS:
    (BASE / filename).write_text(TEMPLATE.format(title=title, theory=theory), encoding="utf-8")

print(f"Generated {len(DOCS)} learning docs in {BASE}")
