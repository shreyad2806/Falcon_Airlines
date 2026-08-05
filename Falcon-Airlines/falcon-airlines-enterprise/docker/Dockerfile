# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package && \
    cp target/falcon-airlines-backend-*.jar target/app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre

# Install curl so the container health check can call the actuator endpoint
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /build/target/app.jar app.jar

# Non-root user
RUN groupadd -r falcon && useradd -r -g falcon falcon
USER falcon

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
