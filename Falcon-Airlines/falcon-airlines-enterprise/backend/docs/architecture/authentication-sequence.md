# Authentication Sequence

## Purpose

This document shows the sequence of calls for a successful login and for an
invalid-credentials failure. All class and method names match the actual
Falcon Airlines Enterprise implementation.

## Successful login

```mermaid
sequenceDiagram
    autonumber
    participant U as User
    participant AC as com.falcon.airlines.controller.AuthController
    participant AS as com.falcon.airlines.service.AuthService
    participant AM as org.springframework.security.authentication.AuthenticationManager
    participant DAOP as org.springframework.security.authentication.dao.DaoAuthenticationProvider
    participant CUDS as com.falcon.airlines.security.service.CustomUserDetailsService
    participant UR as com.falcon.airlines.repository.UserRepository
    participant PG as PostgreSQL
    participant BPE as org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
    participant JS as com.falcon.airlines.security.jwt.JwtService
    participant JP as com.falcon.airlines.security.jwt.JwtProperties
    participant RTR as com.falcon.airlines.repository.RefreshTokenRepository
    participant GEH as com.falcon.airlines.exception.GlobalExceptionHandler

    U->>AC: POST /auth/login
    Note over AC: LoginRequest { usernameOrEmail, password }
    AC->>AS: login(LoginRequest)
    AS->>AM: authenticate(UsernamePasswordAuthenticationToken)
    AM->>DAOP: authenticate(Authentication)
    DAOP->>CUDS: loadUserByUsername(usernameOrEmail)
    CUDS->>UR: findByUsername(usernameOrEmail)
    alt username not found
        CUDS->>UR: findByEmail(usernameOrEmail)
    end
    UR->>PG: SELECT ... FROM users WHERE username / email = ?
    PG-->>UR: User row
    UR-->>CUDS: User
    CUDS->>CUDS: resolveAuthorities(User)
    CUDS->>CUDS: isActive(User)
    CUDS->>CUDS: new UserPrincipal(user, authorities)
    CUDS-->>DAOP: UserPrincipal
    DAOP->>BPE: matches(rawPassword, passwordHash)
    BPE-->>DAOP: true
    DAOP-->>AM: authenticated Authentication
    AM-->>AS: Authentication with UserPrincipal
    AS->>JS: generateAccessToken(UserPrincipal)
    JS->>JP: secret, accessTokenExpiration
    JS-->>AS: signed JWT string
    AS->>AS: build RefreshToken
    Note over AS: UUID, ACTIVE, expiresAt, user, ip, userAgent, deviceInfo
    AS->>RTR: save(RefreshToken)
    RTR-->>AS: persisted RefreshToken
    AS->>AS: user.setLastLoginAt(Instant.now())
    AS->>UR: save(User)
    AS->>AS: TokenResponse.builder()<br/>accessToken, refreshToken, tokenType, etc.
    AS-->>AC: TokenResponse
    AC-->>U: 200 OK ApiResponse<TokenResponse>
```

## Invalid credentials / user not found

```mermaid
sequenceDiagram
    autonumber
    participant U as User
    participant AC as com.falcon.airlines.controller.AuthController
    participant AS as com.falcon.airlines.service.AuthService
    participant AM as org.springframework.security.authentication.AuthenticationManager
    participant DAOP as org.springframework.security.authentication.dao.DaoAuthenticationProvider
    participant CUDS as com.falcon.airlines.security.service.CustomUserDetailsService
    participant UR as com.falcon.airlines.repository.UserRepository
    participant PG as PostgreSQL
    participant BPE as org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
    participant BX as com.falcon.airlines.exception.BaseException
    participant GEH as com.falcon.airlines.exception.GlobalExceptionHandler

    U->>AC: POST /auth/login
    AC->>AS: login(LoginRequest)
    AS->>AM: authenticate(UsernamePasswordAuthenticationToken)
    AM->>DAOP: authenticate(Authentication)
    DAOP->>CUDS: loadUserByUsername(usernameOrEmail)
    CUDS->>UR: findByUsername(usernameOrEmail)
    UR->>PG: SELECT ... FROM users
    PG-->>UR: no match
    CUDS->>UR: findByEmail(usernameOrEmail)
    UR->>PG: SELECT ... FROM users
    PG-->>UR: no match
    CUDS--xDAOP: UsernameNotFoundException
    DAOP--xAM: BadCredentialsException
    AM--xAS: AuthenticationException
    AS->>BX: throw BaseException("Invalid username or password", UNAUTHORIZED, "AUTHENTICATION_ERROR")
    BX-->>GEH: caught by @ExceptionHandler(BaseException.class)
    GEH-->>AC: ApiErrorResponse { 401, AUTHENTICATION_ERROR }
    AC-->>U: 401 UNAUTHORIZED
```

## Component explanations

- **User** — the external HTTP client (web, mobile, API consumer).
- **`AuthController`** — public REST entry point under `/auth`. Receives the
  `LoginRequest`, delegates to `AuthService`, and wraps the result in
  `ApiResponse<TokenResponse>`.
- **`AuthService`** — the application service that orchestrates login. It calls
  the `AuthenticationManager`, builds the JWT access token, creates and persists
  the refresh token, updates `lastLoginAt`, and assembles the `TokenResponse`.
- **`AuthenticationManager`** — Spring Security's central authentication
  coordinator. In this project it is a `ProviderManager` backed by the
  `DaoAuthenticationProvider`.
- **`DaoAuthenticationProvider`** — the provider that retrieves the user through
  `UserDetailsService` and verifies the password with the configured
  `BCryptPasswordEncoder`.
- **`CustomUserDetailsService`** — loads the user by username or email and
  resolves the active `GrantedAuthority` set from roles and permissions.
- **`UserRepository`** — Spring Data JPA repository that executes the SQL
  queries against PostgreSQL through the `User` entity.
- **PostgreSQL** — the underlying relational database that stores users, roles,
  and permissions.
- **`BCryptPasswordEncoder`** — the password hashing implementation that compares
  the raw password with the stored `passwordHash`.
- **`JwtService`** — generates and validates JWTs. During login it builds a
  signed access token containing `sub`, `userId`, and `roles` using the secret
  and expiration from `JwtProperties`.
- **`JwtProperties`** — type-safe configuration record for `jwt.secret`,
  `jwt.accessTokenExpiration`, and `jwt.refreshTokenExpiration`.
- **`RefreshTokenRepository`** — persists the generated refresh token with its
  `token`, `user`, `status`, `expiresAt`, and audit metadata.
- **`GlobalExceptionHandler`** — converts `BaseException` (and other exceptions)
  into the standard `ApiErrorResponse` envelope returned to the client.
