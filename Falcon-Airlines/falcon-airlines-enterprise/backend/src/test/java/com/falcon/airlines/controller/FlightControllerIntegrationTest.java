package com.falcon.airlines.controller;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.dto.request.FlightRequest;
import com.falcon.airlines.dto.request.LoginRequest;
import com.falcon.airlines.dto.response.TokenResponse;
import com.falcon.airlines.enums.FlightStatus;
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

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlightControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_ADMIN = "inttest_flight_admin";
    private static final String PASSWORD = "SecureP@ss1";
    private static final String EMAIL = TEST_ADMIN + "@example.com";

    private String accessToken;
    private Long createdFlightId;

    private static final AtomicInteger FLIGHT_COUNTER = new AtomicInteger(0);
    private static final Instant FLIGHT_BASE = Instant.parse("2030-01-01T00:00:00Z");

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
        // Remove flights created by this test class (FX-..., FA-DUP-..., FA-OVR-...) first to avoid FK issues.
        jdbcTemplate.update("DELETE FROM flights WHERE flight_number LIKE 'FX-%' OR flight_number LIKE 'FA-DUP-%' OR flight_number LIKE 'FA-OVR-%'");
        // Remove child token records before removing the user.
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

    private FlightRequest buildValidRequest() {
        int n = FLIGHT_COUNTER.incrementAndGet();
        long departureOffset = 86400L + (n * 100000L);
        long arrivalOffset = 90000L + (n * 100000L);

        FlightRequest request = new FlightRequest();
        request.setFlightNumber("FX-" + UUID.randomUUID().toString().substring(0, 7));
        request.setOriginAirportId(1L);
        request.setDestinationAirportId(2L);
        request.setAircraftId(1L);
        request.setScheduledDeparture(FLIGHT_BASE.plusSeconds(departureOffset));
        request.setScheduledArrival(FLIGHT_BASE.plusSeconds(arrivalOffset));
        request.setStatus(FlightStatus.SCHEDULED);
        request.setTerminal("T1");
        request.setGate("G1");
        request.setIsActive(true);
        return request;
    }

    @Test
    void shouldCreateFlight() {
        FlightRequest request = buildValidRequest();

        ResponseEntity<ApiResponse<JsonNode>> response = restTemplate.exchange(
                "/api/flights",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().isSuccess()).isTrue();
        createdFlightId = response.getBody().getData().get("id").asLong();
    }

    @Test
    void shouldGetFlightById() throws Exception {
        FlightRequest request = buildValidRequest();
        ResponseEntity<String> create = restTemplate.exchange(
                "/api/flights", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(create.getBody());
        long id = body.get("data").get("id").asLong();

        ResponseEntity<String> get = restTemplate.exchange(
                "/api/flights/{id}", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class, id);

        JsonNode data = objectMapper.readTree(get.getBody());
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data.get("success").asBoolean()).isTrue();
    }

    @Test
    void shouldUpdateFlight() throws Exception {
        FlightRequest request = buildValidRequest();
        ResponseEntity<String> create = restTemplate.exchange(
                "/api/flights", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long id = objectMapper.readTree(create.getBody()).get("data").get("id").asLong();

        FlightRequest update = buildValidRequest();
        update.setFlightNumber("FX-UPDATED");

        ResponseEntity<String> put = restTemplate.exchange(
                "/api/flights/{id}", HttpMethod.PUT,
                new HttpEntity<>(update, authHeaders()), String.class, id);

        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldSoftDeleteFlight() throws Exception {
        FlightRequest request = buildValidRequest();
        ResponseEntity<String> create = restTemplate.exchange(
                "/api/flights", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long id = objectMapper.readTree(create.getBody()).get("data").get("id").asLong();

        ResponseEntity<String> del = restTemplate.exchange(
                "/api/flights/{id}", HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()), String.class, id);

        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> get = restTemplate.exchange(
                "/api/flights/{id}", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class, id);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldSearchFlights() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/flights", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldFilterByOrigin() {
        String url = UriComponentsBuilder.fromPath("/api/flights").queryParam("originAirport", "JFK").toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldFilterByDestination() {
        String url = UriComponentsBuilder.fromPath("/api/flights").queryParam("destinationAirport", "LHR").toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldFilterByStatus() {
        String url = UriComponentsBuilder.fromPath("/api/flights").queryParam("status", FlightStatus.SCHEDULED).toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldFilterByAircraft() {
        String url = UriComponentsBuilder.fromPath("/api/flights").queryParam("aircraft", "VT-IXB").toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldPaginate() {
        String url = UriComponentsBuilder.fromPath("/api/flights")
                .queryParam("page", 0).queryParam("size", 2).toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldSort() {
        String url = UriComponentsBuilder.fromPath("/api/flights")
                .queryParam("sort", "scheduledDeparture,asc").toUriString();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldRejectInvalidOrigin() {
        FlightRequest request = buildValidRequest();
        request.setOriginAirportId(999L);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/flights", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectInvalidAircraft() {
        FlightRequest request = buildValidRequest();
        request.setAircraftId(999L);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/flights", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectSameOriginAndDestination() {
        FlightRequest request = buildValidRequest();
        request.setDestinationAirportId(request.getOriginAirportId());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/flights", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectInvalidSchedule() {
        FlightRequest request = buildValidRequest();
        request.setScheduledArrival(request.getScheduledDeparture().minusSeconds(60));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/flights", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectDuplicateFlight() {
        FlightRequest request = buildValidRequest();
        request.setFlightNumber("FA-DUP-" + UUID.randomUUID().toString().substring(0, 3));
        request.setScheduledDeparture(Instant.now().plusSeconds(200000));
        request.setScheduledArrival(Instant.now().plusSeconds(210000));

        ResponseEntity<String> first = restTemplate.exchange(
                "/api/flights", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> second = restTemplate.exchange(
                "/api/flights", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldRejectAircraftOverlap() {
        FlightRequest request = new FlightRequest();
        request.setFlightNumber("FA-OVR-" + UUID.randomUUID().toString().substring(0, 3));
        request.setOriginAirportId(1L);
        request.setDestinationAirportId(2L);
        request.setAircraftId(1L);
        // Overlaps with FA101: 2026-08-15 08:00 - 20:00
        request.setScheduledDeparture(Instant.parse("2026-08-15T09:00:00Z"));
        request.setScheduledArrival(Instant.parse("2026-08-15T21:00:00Z"));
        request.setStatus(FlightStatus.SCHEDULED);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/flights", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
