package com.falcon.airlines.controller;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.dto.request.LoginRequest;
import com.falcon.airlines.dto.request.PassengerRequest;
import com.falcon.airlines.dto.response.TokenResponse;
import com.falcon.airlines.enums.Gender;
import com.falcon.airlines.response.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PassengerControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_ADMIN = "inttest_passenger_admin";
    private static final String PASSWORD = "SecureP@ss1";
    private static final String EMAIL = TEST_ADMIN + "@example.com";

    private String accessToken;
    private Long createdPassengerId;

    private static final AtomicInteger PASSENGER_COUNTER = new AtomicInteger(0);

    @BeforeAll
    void setUp() {
        String hash = passwordEncoder.encode(PASSWORD);
        jdbcTemplate.update(
                "INSERT INTO users (username, email, password_hash, status, mfa_enabled, failed_login_attempts) VALUES (?, ?, ?, 'ACTIVE', false, 0)",
                TEST_ADMIN, EMAIL, hash);

        Integer userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Integer.class, TEST_ADMIN);
        Integer adminRoleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = 'ADMIN'", Integer.class);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id, valid_from) VALUES (?, ?, now())", userId, adminRoleId);

        LoginRequest login = new LoginRequest();
        login.setUsernameOrEmail(TEST_ADMIN);
        login.setPassword(PASSWORD);

        ResponseEntity<ApiResponse<TokenResponse>> response = restTemplate.exchange(
                "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(login),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        accessToken = "Bearer " + response.getBody().getData().getAccessToken();
    }

    @AfterAll
    void tearDown() {
        jdbcTemplate.update("DELETE FROM passengers WHERE first_name LIKE 'Test-%'");
        jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id = (SELECT id FROM users WHERE username = ?)", TEST_ADMIN);
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username = ?)", TEST_ADMIN);
        jdbcTemplate.update("DELETE FROM users WHERE username = ?", TEST_ADMIN);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken.replace("Bearer ", ""));
        return headers;
    }

    private PassengerRequest buildValidRequest() {
        int n = PASSENGER_COUNTER.incrementAndGet();
        PassengerRequest request = new PassengerRequest();
        request.setUserId(null);
        request.setFirstName("Test-" + n);
        request.setLastName("Passenger");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setEmail("test" + n + "@example.com");
        request.setPhone("+1234567890");
        request.setPassportNumber("PP" + String.format("%07d", n));
        request.setNationality("USA");
        request.setGender(Gender.M);
        request.setRedressNumber(null);
        return request;
    }

    @Test
    void shouldCreatePassenger() {
        PassengerRequest request = buildValidRequest();

        ResponseEntity<ApiResponse<JsonNode>> response = restTemplate.exchange(
                "/api/passengers",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().isSuccess()).isTrue();
        createdPassengerId = response.getBody().getData().get("id").asLong();
    }

    @Test
    void shouldGetPassengerById() throws Exception {
        PassengerRequest request = buildValidRequest();
        ResponseEntity<String> create = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(create.getBody());
        long id = body.get("data").get("id").asLong();

        ResponseEntity<String> get = restTemplate.exchange(
                "/api/passengers/{id}", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class, id);

        JsonNode data = objectMapper.readTree(get.getBody());
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data.get("success").asBoolean()).isTrue();
    }

    @Test
    void shouldUpdatePassenger() throws Exception {
        PassengerRequest request = buildValidRequest();
        ResponseEntity<String> create = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long id = objectMapper.readTree(create.getBody()).get("data").get("id").asLong();

        PassengerRequest update = buildValidRequest();
        update.setFirstName("Updated");

        ResponseEntity<String> put = restTemplate.exchange(
                "/api/passengers/{id}", HttpMethod.PUT,
                new HttpEntity<>(update, authHeaders()), String.class, id);

        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldSoftDeletePassenger() throws Exception {
        PassengerRequest request = buildValidRequest();
        ResponseEntity<String> create = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long id = objectMapper.readTree(create.getBody()).get("data").get("id").asLong();

        ResponseEntity<String> del = restTemplate.exchange(
                "/api/passengers/{id}", HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()), String.class, id);

        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> get = restTemplate.exchange(
                "/api/passengers/{id}", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class, id);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldGetPassengerHistory() throws Exception {
        PassengerRequest request = buildValidRequest();
        ResponseEntity<String> create = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long id = objectMapper.readTree(create.getBody()).get("data").get("id").asLong();

        ResponseEntity<String> history = restTemplate.exchange(
                "/api/passengers/{id}/history", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class, id);

        JsonNode data = objectMapper.readTree(history.getBody());
        assertThat(history.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data.get("success").asBoolean()).isTrue();
    }

    @Test
    void shouldSearchPassengers() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/passengers", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldFilterByFirstName() {
        String url = UriComponentsBuilder.fromPath("/api/passengers").queryParam("firstName", "Test").toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldFilterByLastName() {
        String url = UriComponentsBuilder.fromPath("/api/passengers").queryParam("lastName", "Passenger").toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldFilterByEmail() {
        String url = UriComponentsBuilder.fromPath("/api/passengers").queryParam("email", "test").toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldFilterByPassportNumber() {
        String url = UriComponentsBuilder.fromPath("/api/passengers").queryParam("passportNumber", "PP").toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldPaginate() {
        String url = UriComponentsBuilder.fromPath("/api/passengers")
                .queryParam("page", 0).queryParam("size", 2).toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldSort() {
        String url = UriComponentsBuilder.fromPath("/api/passengers")
                .queryParam("sort", "firstName,asc").toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldRejectInvalidEmail() {
        PassengerRequest request = buildValidRequest();
        request.setEmail("invalid-email");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectInvalidPhone() {
        PassengerRequest request = buildValidRequest();
        request.setPhone("invalid-phone");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectInvalidPassport() {
        PassengerRequest request = buildValidRequest();
        request.setPassportNumber("invalid"); // lowercase violates pattern

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        PassengerRequest request = new PassengerRequest();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectFutureDob() {
        PassengerRequest request = buildValidRequest();
        request.setDateOfBirth(LocalDate.now().plusDays(1));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectDuplicatePassport() throws Exception {
        PassengerRequest request = buildValidRequest();
        ResponseEntity<String> first = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        PassengerRequest duplicate = new PassengerRequest();
        duplicate.setUserId(null);
        duplicate.setFirstName("Duplicate");
        duplicate.setLastName("Passenger");
        duplicate.setDateOfBirth(LocalDate.of(1990, 1, 1));
        duplicate.setEmail("duplicate@example.com");
        duplicate.setPhone("+1234567890");
        duplicate.setPassportNumber(request.getPassportNumber());
        duplicate.setNationality("USA");
        duplicate.setGender(Gender.M);

        ResponseEntity<String> second = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(duplicate, authHeaders()), String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        PassengerRequest request = buildValidRequest();
        ResponseEntity<String> first = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        PassengerRequest duplicate = new PassengerRequest();
        duplicate.setUserId(null);
        duplicate.setFirstName("Duplicate");
        duplicate.setLastName("Passenger");
        duplicate.setDateOfBirth(LocalDate.of(1990, 1, 1));
        duplicate.setEmail(request.getEmail());
        duplicate.setPhone("+1234567890");
        duplicate.setPassportNumber("PP9999999");
        duplicate.setNationality("USA");
        duplicate.setGender(Gender.M);

        ResponseEntity<String> second = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(duplicate, authHeaders()), String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldReturnNotFoundForMissingPassenger() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/passengers/999999", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnNotFoundForMissingPassengerUpdate() {
        PassengerRequest request = buildValidRequest();
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/passengers/999999", HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnNotFoundForMissingPassengerDelete() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/passengers/999999", HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnUnauthorizedWithoutToken() {
        PassengerRequest request = buildValidRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/passengers", HttpMethod.POST,
                new HttpEntity<>(request, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnUnauthorizedForGetWithoutToken() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/passengers", HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
