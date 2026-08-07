package com.falcon.airlines.controller;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.dto.request.LoginRequest;
import com.falcon.airlines.dto.request.PasswordResetConfirmRequest;
import com.falcon.airlines.dto.request.PasswordResetRequest;
import com.falcon.airlines.dto.request.RegisterRequest;
import com.falcon.airlines.dto.response.TokenResponse;
import com.falcon.airlines.dto.response.UserResponse;
import com.falcon.airlines.entity.PasswordResetToken;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.repository.PasswordResetTokenRepository;
import com.falcon.airlines.repository.UserRepository;
import com.falcon.airlines.response.ApiErrorResponse;
import com.falcon.airlines.response.ApiResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the public {@link PasswordResetController} endpoints.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@org.springframework.transaction.annotation.Transactional
class PasswordResetControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                "DELETE FROM password_reset_tokens WHERE user_id IN (SELECT id FROM users WHERE username IN (" + placeholders + "))",
                params);
        jdbcTemplate.update(
                "DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE username IN (" + placeholders + "))",
                params);
        jdbcTemplate.update(
                "DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username IN (" + placeholders + "))",
                params);
        jdbcTemplate.update("DELETE FROM users WHERE username IN (" + placeholders + ")", params);
    }

    @Test
    void shouldGeneratePasswordResetToken() {
        String username = "reset_" + UUID.randomUUID().toString().substring(0, 8);
        createdUsernames.add(username);
        String email = username + "@example.com";

        restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username, email, "SecureP@ss1")),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {});

        PasswordResetRequest request = new PasswordResetRequest();
        request.setUsernameOrEmail(username);

        ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
                "/auth/forgot-password",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        String token = response.getBody().getData();
        assertThat(token).isNotBlank();

        PasswordResetToken saved = passwordResetTokenRepository.findByToken(token).orElseThrow();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void shouldResetPasswordWithValidToken() {
        String username = "reset_" + UUID.randomUUID().toString().substring(0, 8);
        createdUsernames.add(username);
        String email = username + "@example.com";
        String originalPassword = "SecureP@ss1";
        String newPassword = "NewP@ssword123";

        restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username, email, originalPassword)),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {});

        PasswordResetRequest forgotRequest = new PasswordResetRequest();
        forgotRequest.setUsernameOrEmail(username);

        ResponseEntity<ApiResponse<String>> forgotResponse = restTemplate.exchange(
                "/auth/forgot-password",
                HttpMethod.POST,
                new HttpEntity<>(forgotRequest),
                new ParameterizedTypeReference<>() {});

        String token = forgotResponse.getBody().getData();

        PasswordResetConfirmRequest confirmRequest = new PasswordResetConfirmRequest();
        confirmRequest.setToken(token);
        confirmRequest.setNewPassword(newPassword);

        ResponseEntity<ApiResponse<String>> confirmResponse = restTemplate.exchange(
                "/auth/reset-password",
                HttpMethod.POST,
                new HttpEntity<>(confirmRequest),
                new ParameterizedTypeReference<>() {});

        assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmResponse.getBody()).isNotNull();
        assertThat(confirmResponse.getBody().isSuccess()).isTrue();

        assertThat(passwordResetTokenRepository.findByToken(token)).isEmpty();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(username);
        loginRequest.setPassword(newPassword);

        ResponseEntity<ApiResponse<TokenResponse>> loginResponse = restTemplate.exchange(
                "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>() {});

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldRejectResetWithInvalidToken() {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken(UUID.randomUUID().toString());
        request.setNewPassword("NewP@ssword123");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/reset-password",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void shouldRejectResetWithExpiredToken() {
        String username = "reset_" + UUID.randomUUID().toString().substring(0, 8);
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
                INSERT INTO password_reset_tokens
                (created_at, updated_at, deleted_at, is_deleted, token, user_id, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now()),
                null,
                false,
                expiredToken,
                user.getId(),
                Timestamp.from(Instant.now().minusSeconds(120)));

        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken(expiredToken);
        request.setNewPassword("NewP@ssword123");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/reset-password",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void shouldRejectForgotPasswordForUnknownUser() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setUsernameOrEmail("reset_unknown_" + UUID.randomUUID());

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/forgot-password",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("USER_NOT_FOUND");
    }

    @Test
    void shouldRejectResetWithShortPassword() {
        String username = "reset_" + UUID.randomUUID().toString().substring(0, 8);
        createdUsernames.add(username);
        String email = username + "@example.com";

        restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username, email, "SecureP@ss1")),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {});

        PasswordResetRequest forgotRequest = new PasswordResetRequest();
        forgotRequest.setUsernameOrEmail(username);

        ResponseEntity<ApiResponse<String>> forgotResponse = restTemplate.exchange(
                "/auth/forgot-password",
                HttpMethod.POST,
                new HttpEntity<>(forgotRequest),
                new ParameterizedTypeReference<>() {});

        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken(forgotResponse.getBody().getData());
        request.setNewPassword("short");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/reset-password",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("VALIDATION_ERROR");
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
