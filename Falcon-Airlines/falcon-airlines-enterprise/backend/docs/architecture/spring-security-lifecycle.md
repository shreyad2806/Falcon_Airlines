# Spring Security HTTP Request Lifecycle

## Purpose

This document describes the lifecycle of an HTTP request through the Falcon
Airlines Enterprise Spring Security stack. All class and component names are
taken from the actual repository.

## Request lifecycle diagram

```mermaid
graph TD
    CLIENT["HTTP client"]
    TOMCAT["org.springframework.boot.web.embedded.tomcat.TomcatWebServer"]
    CHAIN["SecurityFilterChain<br/>com.falcon.airlines.config.SecurityConfig"]
    JAF["JwtAuthenticationFilter<br/>com.falcon.airlines.security.jwt.JwtAuthenticationFilter"]
    JTU["JwtTokenUtil<br/>com.falcon.airlines.security.jwt.JwtTokenUtil"]
    JS["JwtService<br/>com.falcon.airlines.security.jwt.JwtService"]
    CUDS["CustomUserDetailsService<br/>com.falcon.airlines.security.service.CustomUserDetailsService"]
    UR["UserRepository<br/>com.falcon.airlines.repository.UserRepository"]
    UP["UserPrincipal<br/>com.falcon.airlines.security.principal.UserPrincipal"]
    UPA["UsernamePasswordAuthenticationToken"]
    SCH["SecurityContextHolder<br/>org.springframework.security.core.context.SecurityContextHolder"]
    AUTHZ["Authorization<br/>AuthorizationFilter + @PreAuthorize"]
    CTRL["AdminController<br/>com.falcon.airlines.controller.AdminController"]
    SERVICE["Service layer"]
    REPO["Repository layer<br/>(Spring Data JPA)"]
    PG[(PostgreSQL)]
    GEH["GlobalExceptionHandler<br/>com.falcon.airlines.exception.GlobalExceptionHandler"]

    CLIENT -->|HTTP GET /admin/dashboard| TOMCAT
    TOMCAT -->|dispatch to filter chain| CHAIN
    CHAIN --> JAF
    JAF --> JTU
    JTU -->|Authorization: Bearer ...| JAF
    JAF -->|extractUsername| JS
    JAF -->|isTokenValid| JS
    JS -->|valid?| JAF
    JAF -->|loadUserByUsername| CUDS
    CUDS -->|findByUsername / findByEmail| UR
    UR --> PG
    CUDS -->|construct| UP
    UP --> UPA
    UPA -->|setAuthentication| SCH
    SCH --> JAF
    JAF -->|filterChain.doFilter| AUTHZ
    AUTHZ -->|anyRequest.authenticated + hasRole('ADMIN')| CTRL
    CTRL -->|uses| SERVICE
    SERVICE -->|uses| REPO
    REPO -->|SQL| PG
    PG --> REPO
    REPO --> SERVICE
    SERVICE --> CTRL
    CTRL -->|ApiResponse| CLIENT
```

## Authentication vs authorization

- **Authentication** answers "Who is this user?" It happens in
  `JwtAuthenticationFilter` and the `AuthenticationManager` path.
- **Authorization** answers "Is this user allowed to do this?" It happens in
  `AuthorizationFilter` and the `@PreAuthorize` method-security layer.

## Scenario walkthroughs

### 1. No JWT provided

- `JwtTokenUtil.resolveToken()` returns `Optional.empty()`
- `JwtAuthenticationFilter` calls `filterChain.doFilter(request, response)`
  without touching `SecurityContextHolder`
- `SecurityContextHolder` has no `Authentication`
- `AuthorizationFilter` (enforced by `SecurityFilterChain`) rejects the request
- An `AuthenticationException` is thrown and converted by
  `GlobalExceptionHandler` into an `ApiErrorResponse` with
  `HTTP 401 UNAUTHORIZED` and error code `AUTHENTICATION_ERROR`

### 2. JWT is invalid

- `JwtTokenUtil.resolveToken()` returns the token
- `JwtService.extractUsername()` or `JwtService.isTokenValid()` throws
  `JwtException` or `IllegalArgumentException`
- `JwtAuthenticationFilter` catches the exception, logs a warning, and continues
  the filter chain without setting an `Authentication`
- `SecurityContextHolder` remains empty
- The protected endpoint fails the `anyRequest().authenticated()` check and
  `GlobalExceptionHandler` returns `HTTP 401 UNAUTHORIZED`

### 3. JWT is expired

- `JwtTokenUtil.resolveToken()` extracts the token
- `JwtService.extractExpiration()` shows the expiry is before `Instant.now()`
- `JwtService.isTokenValid()` returns `false`
- `JwtAuthenticationFilter` does not create a `UsernamePasswordAuthenticationToken`
- `SecurityContextHolder` remains empty
- `GlobalExceptionHandler` returns `HTTP 401 UNAUTHORIZED`

### 4. JWT is valid

- `JwtTokenUtil.resolveToken()` extracts the token
- `JwtService.extractUsername()` returns the username
- `JwtService.isTokenValid()` confirms the token is not expired and the
  `subject` matches the loaded user's username
- `JwtAuthenticationFilter` calls `CustomUserDetailsService.loadUserByUsername()`
- `CustomUserDetailsService` loads the `User` from `UserRepository`, resolves
  active roles (`UserRoleRepository`) and permissions (`RolePermissionRepository`)
  and builds a `UserPrincipal` with `SimpleGrantedAuthority` entries
- A `UsernamePasswordAuthenticationToken` is created and stored in
  `SecurityContextHolder.getContext().setAuthentication(...)`
- The request reaches the `AuthorizationFilter` and the `@PreAuthorize` advisor

### 5. User does not have the required role

- The same valid-JWT path runs and `SecurityContextHolder` is populated
- `AuthorizationFilter` lets the request through because an `Authentication`
  exists
- Method security evaluates the `@PreAuthorize` expression on the target
  controller method. For `AdminController.dashboard()` this is
  `@PreAuthorize("hasRole('ADMIN')")`
- `DefaultMethodSecurityExpressionHandler` (configured in `SecurityConfig`)
  uses the `RoleHierarchyImpl` bean (`ROLE_ADMIN > ROLE_AGENT > ROLE_CUSTOMER`)
  to decide whether the authenticated authorities satisfy the expression
- If the user does not have `ROLE_ADMIN` and the hierarchy does not grant it,
  `AuthorizationDeniedException` is thrown
- `GlobalExceptionHandler` catches `AuthorizationDeniedException` or
  `AccessDeniedException` and returns `HTTP 403 FORBIDDEN` with error code
  `ACCESS_DENIED`

## Key components

| Concern | Actual class / interface |
|---------|--------------------------|
| Embedded server | `org.springframework.boot.web.embedded.tomcat.TomcatWebServer` |
| Security filter chain | `com.falcon.airlines.config.SecurityConfig` / `org.springframework.security.web.SecurityFilterChain` |
| JWT extraction | `com.falcon.airlines.security.jwt.JwtTokenUtil` |
| JWT generation/validation | `com.falcon.airlines.security.jwt.JwtService` |
| JWT configuration | `com.falcon.airlines.security.jwt.JwtProperties` |
| User loading | `com.falcon.airlines.security.service.CustomUserDetailsService` |
| Principal object | `com.falcon.airlines.security.principal.UserPrincipal` |
| JWT filter | `com.falcon.airlines.security.jwt.JwtAuthenticationFilter` |
| Authentication token | `org.springframework.security.authentication.UsernamePasswordAuthenticationToken` |
| Security context | `org.springframework.security.core.context.SecurityContextHolder` |
| Role hierarchy | `org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl` |
| Method security expression handler | `org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler` |
| Protected controller example | `com.falcon.airlines.controller.AdminController` |
| Exception mapping | `com.falcon.airlines.exception.GlobalExceptionHandler` |
