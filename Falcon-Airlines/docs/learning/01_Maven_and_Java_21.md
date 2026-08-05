# Maven and Java 21

## What we implemented

- Maven project using `spring-boot-starter-parent` 3.3.2
- Java 21 `release` target
- Spring Boot Maven plugin with the `build-info` goal
- Lombok as the only explicit compile-time annotation processor

## Why

- The parent POM gives tested dependency versions, Java 21 support and consistent plugin versions.
- `build-info` exposes version and build time on `/actuator/info`.
- `release` guarantees byte code only uses APIs from Java 21, preventing accidental newer-API use.

## Important POM snippets

Parent POM:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.2</version>
    <relativePath/>
</parent>
```

Java 21:

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.release>21</maven.compiler.release>
</properties>
```

Build-info for Actuator:

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>build-info</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Common interview questions

1. **Why `maven.compiler.release` instead of `source/target`?**  
   `release` validates both the source and target compatibility and prevents using APIs introduced after the target release.

2. **What does `mvn spring-boot:run -Dspring.profiles.active=dev` do?**  
   Compiles the project, resolves dependencies and starts the application with the `dev` profile.

3. **How does Spring Boot parent manage versions?**  
   Through `dependencyManagement` and `pluginManagement`, so child POMs omit versions for managed artifacts.

## Best practices

- Pin only versions not managed by the parent (e.g. `springdoc.version`, `testcontainers.version`).
- Keep `build-info` enabled for production observability.
- Do not bundle test-scoped or optional dependencies into the runtime jar.
