# Authentication Interview Notes

Concise, interview-ready answers based on the actual Falcon Airlines Enterprise
Spring Security implementation.

## 1. How does JWT authentication work?

**Concept:** A user logs in with credentials and receives a signed token. The
token contains identity and authority claims. For every subsequent request the
client sends the token in the `Authorization: Bearer <token>` header. The server
validates the signature and expiry instead of keeping session state.

**Falcon Airlines:**
- `AuthController.login(LoginRequest)` returns `ApiResponse<TokenResponse>`
  with an access token and a refresh token.
- `JwtService.generateAccessToken(UserPrincipal)` builds the token with `sub`
  (username), `userId`, and `roles` claims, signed with `JwtProperties.secret`.
- `JwtAuthenticationFilter` intercepts requests, uses `JwtTokenUtil.resolveToken`
  to extract the token, and calls `JwtService.isTokenValid` before setting the
  `SecurityContext`.

## 2. Why use JWT?

**Concept:** JWTs are compact, self-contained, and can carry claims. They allow
stateless authentication where the server does not need to query a session store
for every request.

**Falcon Airlines:** The backend is configured as
`SessionCreationPolicy.STATELESS` in `SecurityConfig`. `JwtService` validates
locally against the signing key, so the `JwtAuthenticationFilter` does not need
to call a database to check the token.

## 3. Why is JWT considered stateless?

**Concept:** The server does not maintain a session object for each user. All
the information needed to verify the request is inside the JWT.

**Falcon Airlines:** `SecurityConfig` disables sessions, and `JwtAuthenticationFilter`
reconstructs the `Authentication` from the JWT on every request. The server only
stores long-lived refresh tokens; it never keeps an in-memory or server-side
session for logged-in users.

## 4. What is the difference between authentication and authorization?

**Concept:** Authentication is "who are you?" — verifying credentials.
Authorization is "what are you allowed to do?" — checking permissions.

**Falcon Airlines:**
- **Authentication:** `JwtAuthenticationFilter` + `JwtService` + `CustomUserDetailsService`.
- **Authorization:** `SecurityFilterChain` (`anyRequest().authenticated()`) plus
  method-level `@PreAuthorize` on `AdminController`.

## 5. What happens during POST /auth/login?

**Concept:** The client sends username/email and password. The server verifies
credentials, generates an access token and a refresh token, and returns them.

**Falcon Airlines:**
- `AuthController.login` calls `AuthService.login(LoginRequest)`.
- `AuthService` uses `AuthenticationManager` to authenticate the credentials.
- On success it calls `JwtService.generateAccessToken(UserPrincipal)`.
- It creates a `RefreshToken` (UUID, `ACTIVE`, `expiresAt`) and saves it with
  `RefreshTokenRepository.save`.
- It returns a `TokenResponse` containing `accessToken` and `refreshToken`.

## 6. What is AuthenticationManager?

**Concept:** It is the central Spring Security component that delegates
credential verification to one or more `AuthenticationProvider`s.

**Falcon Airlines:** `SecurityConfig` creates a `ProviderManager` bean with a
`DaoAuthenticationProvider`. `AuthService.login` calls
`AuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken(...))`.

## 7. What is UserDetailsService?

**Concept:** It loads user data (username, password, authorities) from a data
store when Spring Security needs to authenticate or validate a token.

**Falcon Airlines:**
- `CustomUserDetailsService` implements `UserDetailsService`.
- `loadUserByUsername(String)` tries `UserRepository.findByUsername` and then
  `UserRepository.findByEmail`.
- It builds a `UserPrincipal` with `SimpleGrantedAuthority` entries for active
  roles and permissions.

## 8. What is SecurityContext?

**Concept:** It is a Spring Security holder that stores the current
`Authentication` for the duration of the request thread.

**Falcon Airlines:** `JwtAuthenticationFilter` creates a
`UsernamePasswordAuthenticationToken`, sets its details, and calls
`SecurityContextHolder.getContext().setAuthentication(authentication)`. Later
filters and controllers can access `SecurityContextHolder` to get the
authenticated user.

## 9. What does SecurityFilterChain do?

**Concept:** It defines the chain of servlet filters that every request must
pass through — including security filters, authorization rules, and exception
handling.

**Falcon Airlines:** `SecurityConfig.securityFilterChain`:
- Disables CSRF and sessions (`STATELESS`)
- Adds `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
- Permits `/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`
- Requires authentication for everything else

## 10. What does a JWT filter do?

**Concept:** It extracts a JWT from the `Authorization` header, validates it,
and, if valid, populates the `SecurityContext` with the authenticated principal.

**Falcon Airlines:** `JwtAuthenticationFilter`:
- Uses `JwtTokenUtil.resolveToken` to get the JWT
- Calls `JwtService.extractUsername` and `JwtService.isTokenValid`
- Loads the user through `UserDetailsService`
- Builds a `UsernamePasswordAuthenticationToken` and stores it in
  `SecurityContextHolder`

## 11. Why use BCrypt?

**Concept:** BCrypt is a slow, adaptive hashing algorithm designed for passwords.
It includes a salt and a work factor, making brute-force and rainbow-table
attacks expensive.

**Falcon Airlines:** `SecurityConfig` registers a `BCryptPasswordEncoder` bean.
`AuthService.register` stores `passwordEncoder.encode(rawPassword)` in the
`password_hash` column. `DaoAuthenticationProvider` uses the same bean to match
raw passwords against the stored hash.

## 12. Why should passwords never be stored directly?

**Concept:** Storing plain text means anyone with database access can use the
credentials. Hashing makes it computationally hard to recover the original
password.

**Falcon Airlines:** `User.passwordHash` stores only the BCrypt hash. The
`BCryptPasswordEncoder` in `SecurityConfig` never stores or compares plain
passwords; it compares hashes.

## 13. What is an access token?

**Concept:** A short-lived token that grants access to protected APIs. It is
cheap to validate but has a limited lifetime.

**Falcon Airlines:** `JwtService.generateAccessToken(UserPrincipal)` issues an
access token with the lifetime from `JwtProperties.accessTokenExpiration`
(default 900 seconds). The client uses it in the `Authorization: Bearer` header.

## 14. What is a refresh token?

**Concept:** A long-lived credential stored on the server that can be exchanged
for a new access token without re-entering the user's password.

**Falcon Airlines:** `AuthService.login` creates a `RefreshToken` entity with a
UUID, stores it in PostgreSQL via `RefreshTokenRepository`, and returns the token
string in `TokenResponse`. It expires after `JwtProperties.refreshTokenExpiration`
(default 604 800 seconds).

## 15. Why have two tokens?

**Concept:** Separation of concerns. Access tokens are short-lived and stateless,
so they can be validated quickly on every request. Refresh tokens are long-lived
and server-side, so they can be revoked.

**Falcon Airlines:** The stateless access token (JWT) carries identity and
authority claims. The refresh token (database row) allows the client to request
new access tokens and can be rotated or deleted on logout.

## 16. What happens when an access token expires?

**Concept:** The server rejects the request (401), and the client must get a new
access token, usually with a refresh token.

**Falcon Airlines:** `JwtService.isTokenValid` checks `expiration.isAfter(Instant.now())`.
If the access token is expired, `JwtAuthenticationFilter` will not set the
`SecurityContext`. The client then calls `POST /auth/refresh` with the refresh
token to get a new `TokenResponse`.

## 17. How does refresh token rotation work?

**Concept:** Each time a refresh token is used, it is invalidated and a brand-new
one is issued. This reduces the impact of a stolen refresh token.

**Falcon Airlines:** `AuthService.refresh(RefreshTokenRequest)`:
- Loads the old token and validates it (`ACTIVE`, not expired, not deleted, user active)
- Sets `status = REVOKED` and `revokedAt = Instant.now()`
- Generates a new access token and a new `RefreshToken` (new UUID, `ACTIVE`)
- Saves both and returns a new `TokenResponse`
- Any later reuse of the old token returns `INVALID_REFRESH_TOKEN` (401)

## 18. How does logout work with JWT?

**Concept:** Since JWTs are stateless, you cannot truly "delete" an access token.
Logout is implemented by invalidating the refresh token so it can no longer be
used, and by discarding the access token on the client.

**Falcon Airlines:** `AuthController.logout(LogoutRequest)` calls
`AuthService.logout(LogoutRequest)`, which finds the refresh token in
`RefreshTokenRepository` and calls `delete(token)` to remove the row from
PostgreSQL. Subsequent refresh attempts with that token fail with 401.

## 19. How does role-based authorization work?

**Concept:** Users are assigned roles. Each role maps to a set of permissions.
The framework checks whether the authenticated user has the required role or
authority before allowing a method to run.

**Falcon Airlines:**
- Roles: `ADMIN`, `AGENT`, `CUSTOMER` (`com.falcon.airlines.enums.Role`)
- Permissions: `BOOKING_READ`, `FLIGHT_WRITE`, etc. (`com.falcon.airlines.enums.Permission`)
- `CustomUserDetailsService` builds `SimpleGrantedAuthority` entries such as
  `ROLE_CUSTOMER` and `BOOKING_READ`
- `SecurityConfig` registers a `RoleHierarchy`: `ROLE_ADMIN > ROLE_AGENT > ROLE_CUSTOMER`
- `AdminController` uses `@PreAuthorize("hasRole('ADMIN')")` and
  `@PreAuthorize("hasAnyAuthority('BOOKING_READ', 'BOOKING_WRITE')")`

## 20. Difference between 401 and 403?

**Concept:**
- `401 Unauthorized` — the request lacks valid authentication.
- `403 Forbidden` — the client is authenticated but not allowed to access the
  resource.

**Falcon Airlines:** `GlobalExceptionHandler` maps:
- `AuthenticationException` / `BaseException` authentication errors → `401`
- `AuthorizationDeniedException` / `AccessDeniedException` → `403` with
  error code `ACCESS_DENIED`

## 21. What is @PreAuthorize?

**Concept:** It is a Spring Security method-level annotation that evaluates a
SpEL expression before the method is invoked. If the expression is false, an
`AccessDeniedException` is thrown.

**Falcon Airlines:** `AdminController` uses:
- `@PreAuthorize("hasRole('ADMIN')")`
- `@PreAuthorize("hasAnyAuthority('BOOKING_READ', 'BOOKING_WRITE')")`
- `@PreAuthorize("hasRole('CUSTOMER')")`

These are evaluated by `DefaultMethodSecurityExpressionHandler` with the
configured `RoleHierarchy`.

## 22. Why use stateless sessions?

**Concept:** Stateless servers scale horizontally. They do not need to share
session state across instances. This fits microservices and 12-factor apps.

**Falcon Airlines:** `SecurityConfig` sets
`session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)`. Authentication
state lives in the JWT, so any application instance can validate the request
using only the signing key.

## 23. What is CSRF and why is it configured differently for REST APIs?

**Concept:** CSRF (Cross-Site Request Forgery) tricks an authenticated browser
user into performing unwanted actions. For REST APIs that use `Authorization:
Bearer` headers, CSRF protection is unnecessary because the token is not sent by
the browser automatically.

**Falcon Airlines:** `SecurityConfig` disables CSRF with `.csrf(csrf -> csrf.disable())`.
The API relies on the JWT access token sent by the client.

## 24. Where are authentication credentials stored?

**Concept:** User identity is stored in PostgreSQL. Passwords are stored as
hashes, and refresh tokens are stored as database records. JWT access tokens are
stateless and only exist in the client or in-transit.

**Falcon Airlines:**
- Users: `users` table
- Passwords: `users.password_hash` (BCrypt)
- Refresh tokens: `refresh_tokens` table (`RefreshToken` entity)
- Roles/permissions: `roles`, `permissions`, `user_roles`, `role_permissions`

## 25. What security vulnerabilities should be considered?

**Concepts and Falcon Airlines mitigations:**
- **Credential stuffing / brute force** — BCrypt slows hash attempts; account
  lockout fields (`failed_login_attempts`, `locked_until`) are on the `User`
  entity.
- **Token theft** — short-lived access tokens (15 min) and refresh-token rotation
  reduce exposure.
- **Replay attacks** — tokens are signed; expiry prevents indefinite reuse.
- **CSRF** — disabled for the stateless REST API; no browser cookies used for
  authentication.
- **Privilege escalation** — role hierarchy and explicit `@PreAuthorize` checks
  limit access.
- **Leaked signing secret** — `JwtProperties` enforces a secret of at least 32
  bytes.
- **Expired refresh tokens** — the `RefreshTokenRepository` has cleanup queries,
  but a scheduled job is not currently wired. (TODO in the current implementation.)

## Explain the entire authentication system in 60 seconds

"Falcon Airlines uses stateless JWT authentication on top of Spring Security 6.

When a user logs in, `AuthController.login` passes the request to `AuthService`,
which uses `AuthenticationManager` and `DaoAuthenticationProvider` to verify
credentials with `CustomUserDetailsService` and `BCryptPasswordEncoder`.

On success, `JwtService` issues a short-lived access token and `AuthService`
creates a long-lived `RefreshToken` stored in PostgreSQL. The client sends the
access token in the `Authorization` header. For each request,
`JwtAuthenticationFilter` extracts the token with `JwtTokenUtil`, validates it
with `JwtService`, and loads the current user back into `SecurityContextHolder`.

For authorization, `SecurityConfig` exposes `/auth/**` publicly while requiring
authentication for everything else. `AdminController` uses `@PreAuthorize` with
`hasRole` and `hasAnyAuthority`, backed by a role hierarchy and a
`DefaultMethodSecurityExpressionHandler`.

If an access token expires, the client calls `POST /auth/refresh`; the old
refresh token is revoked and a brand-new token pair is issued. Logging out
deletes the refresh token from the database." .
