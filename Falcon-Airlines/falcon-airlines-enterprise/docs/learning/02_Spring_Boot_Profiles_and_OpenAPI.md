# Spring Boot Profiles and OpenAPI

## What we implemented

- `application.yml` base configuration with default `dev` profile
- `application-dev.yml`, `application-prod.yml`, `application-test.yml`
- SpringDoc OpenAPI/Swagger UI integration

## Why

- Profiles separate runtime behaviour by environment without code changes.
- OpenAPI provides a discoverable, interactive API contract for developers and consumers.

## Important annotations and YAML keys

Base profile activation in `application.yml`:

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

Profile-specific file in `application-dev.yml`:

```yaml
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/falcon_airlines}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:postgres}
```

OpenAPI bean from `OpenApiConfig.java`:

```java
@Bean
public OpenAPI falconAirlinesOpenApi() {
    return new OpenAPI()
            .info(new Info()
                    .title("Falcon Airlines Enterprise API")
                    .version("v1.0.0"));
}
```

## Common interview questions

1. **How does Spring Boot choose a profile?**  
   Via `spring.profiles.active` property, JVM argument `-Dspring.profiles.active=dev`, or environment variable `SPRING_PROFILES_ACTIVE`.

2. **What is the precedence of `application*.yml` files?**  
   Profile-specific files override the base `application.yml`. Later profiles can override earlier ones when multiple are active.

3. **Where is Swagger UI exposed?**  
   `/swagger-ui.html` by default when `springdoc-openapi-starter-webmvc-ui` is on the classpath.

## Best practices

- Never commit secrets in YAML files; use environment variables or a secrets manager.
- Keep `spring.jpa.hibernate.ddl-auto: none` in production and own the schema with Flyway.
- Use `spring.config.activate.on-profile` in dedicated profile files instead of `---` document separators in one file.
