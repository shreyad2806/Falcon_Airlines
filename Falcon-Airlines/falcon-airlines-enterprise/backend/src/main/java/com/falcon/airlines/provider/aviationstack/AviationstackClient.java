package com.falcon.airlines.provider.aviationstack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;

/**
 * REST client for Aviationstack flight data API.
 */
@Slf4j
@Component
public class AviationstackClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    public AviationstackClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${aviationstack.api.key:}") String apiKey,
            @Value("${aviationstack.base-url:http://api.aviationstack.com/v1}") String baseUrl) {

        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Search flights from Aviationstack.
     * Returns empty list on any error (never throws to caller).
     */
    public List<AviationstackFlight> searchFlights(String origin, String destination,
                                                     String date, String flightNumber) {
        if (!isConfigured()) {
            log.warn("Aviationstack API key not configured");
            return List.of();
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/flights")
                    .queryParam("access_key", apiKey)
                    .queryParam("limit", 100);

            if (origin != null && !origin.isBlank()) {
                builder.queryParam("dep_iata", origin.toUpperCase());
            }
            if (destination != null && !destination.isBlank()) {
                builder.queryParam("arr_iata", destination.toUpperCase());
            }
            if (flightNumber != null && !flightNumber.isBlank()) {
                builder.queryParam("flight_iata", flightNumber.toUpperCase());
            }
            if (date != null && !date.isBlank()) {
                builder.queryParam("flight_date", date);
            }

            String url = builder.toUriString();
            log.debug("Aviationstack request: {}", url.replaceAll("access_key=.*?(?=&|$)", "access_key=***"));

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<AviationstackResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), AviationstackResponse.class);

            if (response.getBody() != null && response.getBody().getData() != null) {
                List<AviationstackFlight> flights = response.getBody().getData();
                log.info("Aviationstack returned {} flights", flights.size());
                return flights;
            }

            return List.of();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("Aviationstack rate limit exceeded");
            } else if (e.getStatusCode().value() == 401) {
                log.error("Aviationstack API key is invalid or expired");
            } else {
                log.warn("Aviationstack HTTP error {}: {}", e.getStatusCode().value(), e.getMessage());
            }
            return List.of();
        } catch (ResourceAccessException e) {
            log.warn("Aviationstack API timeout or connection error: {}", e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Aviationstack API error: {}", e.getMessage());
            return List.of();
        }
    }

    public List<AviationstackFlight> getFlightStatus(String flightNumber, String date) {
        return searchFlights(null, null, date, flightNumber);
    }

    // ---- Aviationstack response DTOs ----

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AviationstackResponse {
        private boolean success;
        private String error;
        private List<AviationstackFlight> data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AviationstackFlight {
        @JsonProperty("flight_date")
        private String flightDate;
        @JsonProperty("flight_status")
        private String flightStatus;
        private AviationstackAirline airline;
        private AviationstackFlightInfo flight;
        private AviationstackAirport departure;
        private AviationstackAirport arrival;
        private AviationstackAircraft aircraft;
        private AviationstackRealTime live;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AviationstackAirline {
        private String name;
        private String iata;
        private String icao;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AviationstackFlightInfo {
        private String iata;
        private String icao;
        private String number;
        @JsonProperty("codeshared")
        private AviationstackFlightInfo codeshared;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AviationstackAirport {
        private String iata;
        private String icao;
        private String airport;
        private String timezone;
        private AviationstackTime scheduled;
        private AviationstackTime estimated;
        private AviationstackTime actual;
        private String gate;
        private String terminal;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AviationstackTime {
        private String utc;
        private String local;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AviationstackAircraft {
        private String iata;
        private String icao;
        private String registration;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AviationstackRealTime {
        private String altitude;
        private String speed;
        @JsonProperty("is_ground")
        private Boolean isGround;
    }
}
