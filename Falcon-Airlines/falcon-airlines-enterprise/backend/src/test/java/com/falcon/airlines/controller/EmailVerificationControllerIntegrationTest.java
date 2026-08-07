package com.falcon.airlines.controller;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.dto.request.RegisterRequest;
import com.falcon.airlines.dto.request.ResendVerificationRequest;
import com.falcon.airlines.dto.request.VerifyEmailRequest;
import com.falcon.airlines.dto.response.UserResponse;
import com.falcon.airlines.entity.EmailVerificationToken;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.repository.EmailVerificationTokenRepository;
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
 * Integration tests for the public {@link EmailVerificationController} endpoints.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@org.springframework.transaction.annotation.Transactional
class EmailVerificationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

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
                "DELETE FROM email_verification_tokens WHERE user_id IN (SELECT id FROM users WHERE username IN (" + placeholders + "))",
                params);
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
    void shouldGenerateVerificationToken() {
        String username = "verify_" + UUID.randomUUID().toString().substring(0, 8);
        createdUsernames.add(username);
        String email = username + "@example.com";

        restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username, email, "SecureP@ss1")),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {});

        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setUsernameOrEmail(username);

        ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
                "/auth/resend-verification",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();

        String token = response.getBody().getData();
        assertThat(token).isNotBlank();

        EmailVerificationToken saved = emailVerificationTokenRepository.findByToken(token).orElseThrow();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void shouldVerifyEmailWithValidToken() {
        String username = "verify_" + UUID.randomUUID().toString().substring(0, 8);
        createdUsernames.add(username);
        String email = username + "@example.com";

        restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(buildRegisterRequest(username, email, "SecureP@ss1")),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {});

        ResendVerificationRequest resendRequest = new ResendVerificationRequest();
        resendRequest.setUsernameOrEmail(username);

        ResponseEntity<ApiResponse<String>> resendResponse = restTemplate.exchange(
                "/auth/resend-verification",
                HttpMethod.POST,
                new HttpEntity<>(resendRequest),
                new ParameterizedTypeReference<>() {});

        String token = resendResponse.getBody().getData();

        VerifyEmailRequest verifyRequest = new VerifyEmailRequest();
        verifyRequest.setToken(token);

        ResponseEntity<ApiResponse<String>> verifyResponse = restTemplate.exchange(
                "/auth/verify-email",
                HttpMethod.POST,
                new HttpEntity<>(verifyRequest),
                new ParameterizedTypeReference<>() {});

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verifyResponse.getBody()).isNotNull();
        assertThat(verifyResponse.getBody().isSuccess()).isTrue();

        assertThat(emailVerificationTokenRepository.findByToken(token)).isEmpty();

        User user = userRepository.findByUsername(username).orElseThrow();
        assertThat(user.getEmailVerified()).isTrue();
    }

    @Test
    void shouldRejectVerifyWithInvalidToken() {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(UUID.randomUUID().toString());

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/verify-email",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void shouldRejectVerifyWithExpiredToken() {
        String username = "verify_" + UUID.randomUUID().toString().substring(0, 8);
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
                INSERT INTO email_verification_tokens
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

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(expiredToken);

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/verify-email",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void shouldRejectResendForUnknownUser() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setUsernameOrEmail("verify_unknown_" + UUID.randomUUID());

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/auth/resend-verification",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("USER_NOT_FOUND");
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
