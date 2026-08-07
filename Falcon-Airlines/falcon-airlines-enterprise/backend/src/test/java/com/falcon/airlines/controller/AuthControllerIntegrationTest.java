package com.falcon.airlines.controller;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.dto.request.LoginRequest;
import com.falcon.airlines.dto.request.LogoutRequest;
import com.falcon.airlines.dto.request.RefreshTokenRequest;
import com.falcon.airlines.dto.request.RegisterRequest;
import com.falcon.airlines.dto.response.TokenResponse;
import com.falcon.airlines.dto.response.UserResponse;
import com.falcon.airlines.entity.RefreshToken;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.enums.TokenStatus;
import com.falcon.airlines.enums.UserStatus;
import com.falcon.airlines.repository.RefreshTokenRepository;
import com.falcon.airlines.repository.UserRepository;
import com.falcon.airlines.response.ApiErrorResponse;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.security.jwt.JwtService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the public {@link AuthController} registration endpoint.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@org.springframework.transaction.annotation.Transactional
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final List<String> createdUsernames = new ArrayList<>();

    @BeforeAll
    void configureRestTemplate() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @AfterAll
    void tearDown() {
        if (createdUsernames.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", Collections.nCopies(createdUsernames.size(), "?"));
        Object[] params = createdUsernames.toArray();

        jdbcTemplate.update(
                "DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE username IN (" + placeholders + "))",
                params);
        jdbcTemplate.update(
                "DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username IN (" + placeholders + "))",
                params);
        jdbcTemplate.update("DELETE FROM users WHERE username IN (" + placeholders + ")", params);
    }

    @Test
    void shouldRegisterCustomerSuccessfully() {
        String username = "cust_" + UUID.randomUUID().toString().substring(0, 8);
        createdUsernames.add(username);
        String email = username + "@example.com";
        RegisterRequest request = buildRegisterRequest(username, email, "SecureP@ss1");

        ResponseEntity<ApiResponse<UserResponse>> response = restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getUsername()).isEqualTo(username);
        assertThat(response.getBody().getData().getEmail()).isEqualTo(email);
        assertThat(response.getBody().getData().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.getBody().getData().getMfaEnabled()).isFalse();

        User saved = userRepository.findByUsername(username).orElseThrow();
        assertThat(passwordEncoder.matches("SecureP@ss1", saved.getPasswordHash())).isTrue();

        Integer customerRoleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles ur " +
                        "JOIN roles r ON r.id = ur.role_id " +
                        "JOIN users u ON u.id = ur.user_id " +
                        "WHERE u.username = ? AND r.name = 'CUSTOMER' AND u.is_deleted = false",
                Integer.class,
                username);
        assertThat(customerRoleCount).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateUsername() {
        String username = "dupuser_" + UUID.randomUUID().toString().substring(0, 8);
        createdUsernames.add(username);
        String email1 = username + "1@example.com";
        String email2 = username + "2@example.com";

        restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username, email1, "SecureP@ss1")),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {});

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username, email2, "SecureP@ss2")),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("DUPLICATE_RESOURCE");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        String email = "dupemail_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String username1 = "u1_" + UUID.randomUUID().toString().substring(0, 6);
        String username2 = "u2_" + UUID.randomUUID().toString().substring(0, 6);
        createdUsernames.add(username1);
        createdUsernames.add(username2);

        restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username1, email, "SecureP@ss1")),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {});

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username2, email, "SecureP@ss2")),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getError()).isEqualTo("DUPLICATE_RESOURCE");
    }

    @Test
    void shouldLoginRegisteredUser() {
        String username = "login_" + UUID.randomUUID().toString().substring(0, 8);
        createdUsernames.add(username);
        String email = username + "@example.com";
        String password = "SecureP@ss1";

        restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username, email, password)),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {});

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(username);
        loginRequest.setPassword(password);

        ResponseEntity<ApiResponse<TokenResponse>> response = restTemplate.exchange(
                "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        TokenResponse data = response.getBody().getData();
        assertThat(data.getTokenType()).isEqualTo("Bearer");
        assertThat(data.getAccessToken()).isNotBlank();
        assertThat(data.getRefreshToken()).isNotBlank();
        assertThat(data.getExpiresIn()).isEqualTo(900L);
        assertThat(data.getUsername()).isEqualTo(username);
        assertThat(jwtService.extractUsername(data.getAccessToken())).isEqualTo(username);
    }

    @Test
    void shouldRejectLoginWithInvalidCredentials() {
        String username = "badlogin_" + UUID.randomUUID().toString().substring(0, 8);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(username);
        loginRequest.setPassword("WrongP@ss1");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("AUTHENTICATION_ERROR");
    }

    @Test
    void shouldRejectInvalidRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ab");
        request.setEmail("not-an-email");
        request.setPassword("123");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void shouldRefreshAccessToken() {
        String username = "login_" + UUID.randomUUID().toString().substring(0, 8);
        createdUsernames.add(username);
        String email = username + "@example.com";
        String password = "SecureP@ss1";

        restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username, email, password)),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {});

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(username);
        loginRequest.setPassword(password);

        ResponseEntity<ApiResponse<TokenResponse>> loginResponse = restTemplate.exchange(
                "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>() {});

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TokenResponse firstToken = loginResponse.getBody().getData();

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(firstToken.getRefreshToken());

        ResponseEntity<ApiResponse<TokenResponse>> refreshResponse = restTemplate.exchange(
                "/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshRequest),
                new ParameterizedTypeReference<>() {});

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull();
        assertThat(refreshResponse.getBody().isSuccess()).isTrue();

        TokenResponse secondToken = refreshResponse.getBody().getData();
        assertThat(secondToken.getTokenType()).isEqualTo("Bearer");
        assertThat(secondToken.getAccessToken()).isNotBlank();
        assertThat(secondToken.getRefreshToken()).isNotBlank();
        assertThat(secondToken.getRefreshToken()).isNotEqualTo(firstToken.getRefreshToken());
        assertThat(secondToken.getRefreshTokenStatus()).isEqualTo(TokenStatus.ACTIVE);
        assertThat(jwtService.extractUsername(secondToken.getAccessToken())).isEqualTo(username);

        RefreshToken oldToken = refreshTokenRepository.findByToken(firstToken.getRefreshToken()).orElseThrow();
        assertThat(oldToken.getStatus()).isEqualTo(TokenStatus.REVOKED);

        RefreshToken newToken = refreshTokenRepository.findByToken(secondToken.getRefreshToken()).orElseThrow();
        assertThat(newToken.getStatus()).isEqualTo(TokenStatus.ACTIVE);
    }

    @Test
    void shouldRejectRefreshWithInvalidToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(UUID.randomUUID().toString());

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void shouldRejectRefreshWithExpiredToken() {
        String username = "login_" + UUID.randomUUID().toString().substring(0, 8);
        createdUsernames.add(username);
        String email = username + "@example.com";

        restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username, email, "SecureP@ss1")),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {});

        User user = userRepository.findByUsername(username).orElseThrow();
        String expiredToken = UUID.randomUUID().toString();

        jdbcTemplate.update(
                """
                INSERT INTO refresh_tokens
                (created_at, updated_at, deleted_at, is_deleted, token, user_id, ip_address, device_info, user_agent, expires_at, last_used_at, revoked_at, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                java.sql.Timestamp.from(Instant.now()),
                java.sql.Timestamp.from(Instant.now()),
                null,
                false,
                expiredToken,
                user.getId(),
                null,
                null,
                null,
                java.sql.Timestamp.from(Instant.now().minusSeconds(120)),
                null,
                null,
                TokenStatus.ACTIVE.name());

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(expiredToken);

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void shouldRejectRefreshWithRevokedToken() {
        String username = "login_" + UUID.randomUUID().toString().substring(0, 8);
        createdUsernames.add(username);
        String email = username + "@example.com";
        String password = "SecureP@ss1";

        restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username, email, password)),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {});

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(username);
        loginRequest.setPassword(password);

        ResponseEntity<ApiResponse<TokenResponse>> loginResponse = restTemplate.exchange(
                "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>() {});

        TokenResponse firstToken = loginResponse.getBody().getData();

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(firstToken.getRefreshToken());

        restTemplate.exchange(
                "/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(refreshRequest),
                new ParameterizedTypeReference<>() {});

        RefreshTokenRequest reuseRequest = new RefreshTokenRequest();
        reuseRequest.setRefreshToken(firstToken.getRefreshToken());

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/refresh",
                HttpMethod.POST,
                new HttpEntity<>(reuseRequest),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void shouldLogoutAndDeleteRefreshToken() {
        String username = "login_" + UUID.randomUUID().toString().substring(0, 8);
        createdUsernames.add(username);
        String email = username + "@example.com";
        String password = "SecureP@ss1";

        restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username, email, password)),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {});

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(username);
        loginRequest.setPassword(password);

        ResponseEntity<ApiResponse<TokenResponse>> loginResponse = restTemplate.exchange(
                "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>() {});

        TokenResponse data = loginResponse.getBody().getData();
        String refreshToken = data.getRefreshToken();
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isPresent();

        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setRefreshToken(refreshToken);

        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                "/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(logoutRequest),
                Void.class);

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logoutResponse.getBody()).isNull();
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
    }

    @Test
    void shouldReturnNoContentForUnknownToken() {
        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setRefreshToken(UUID.randomUUID().toString());

        ResponseEntity<Void> response = restTemplate.exchange(
                "/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(logoutRequest),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    private RegisterRequest buildRegisterRequest(String username, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setMobileNumber("+91-" + UUID.randomUUID().toString().substring(0, 10));
        return request;
    }
}
