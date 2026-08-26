package com.falcon.airlines.controller;

import com.falcon.airlines.provider.NormalizedFlight;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.service.FlightSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flight search endpoints for the customer-facing booking flow.
 * Uses the FlightDataProvider abstraction for flexible data sourcing.
 */
@Tag(name = "Flight Search", description = "Search and discover flights for booking")
@RestController
@RequestMapping("/api/flights/search")
public class FlightSearchController {

    private final FlightSearchService flightSearchService;

    public FlightSearchController(FlightSearchService flightSearchService) {
        this.flightSearchService = flightSearchService;
    }

    @Operation(summary = "Search flights for booking")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('FLIGHT_READ')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchFlights(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String flightNumber,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Default date = today if not specified
        if (date == null) {
            date = LocalDate.now();
        }

        List<NormalizedFlight> flights = flightSearchService.searchFlights(
                origin, destination, date, flightNumber, status, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("content", flights);
        response.put("totalElements", flights.size());
        response.put("page", page);
        response.put("size", size);
        response.put("provider", flightSearchService.getProviderName());

        return ResponseEntity.ok(ApiResponse.ok("Flights retrieved successfully", response));
    }

    @Operation(summary = "Get live flight status")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{flightNumber}")
    @PreAuthorize("hasAnyAuthority('FLIGHT_READ')")
    public ResponseEntity<ApiResponse<NormalizedFlight>> getFlightStatus(
            @PathVariable String flightNumber,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}") 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return flightSearchService.getFlightStatus(flightNumber, date)
                .map(f -> ResponseEntity.ok(ApiResponse.ok("Flight retrieved successfully", f)))
                .orElse(ResponseEntity.ok(ApiResponse.ok("Flight not found", null)));
    }

    @Operation(summary = "Refresh flight status (clear cache)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{flightNumber}/refresh")
    @PreAuthorize("hasAnyAuthority('FLIGHT_READ')")
    public ResponseEntity<ApiResponse<NormalizedFlight>> refreshFlightStatus(
            @PathVariable String flightNumber) {

        return flightSearchService.refreshFlightStatus(flightNumber)
                .map(f -> ResponseEntity.ok(ApiResponse.ok("Flight status refreshed", f)))
                .orElse(ResponseEntity.ok(ApiResponse.ok("Flight not found", null)));
    }

    @Operation(summary = "Get provider health status")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> getProviderHealth() {
        Map<String, String> health = new HashMap<>();
        health.put("provider", flightSearchService.getProviderName());
        health.put("status", flightSearchService.isProviderAvailable() ? "UP" : "DOWN");
        return ResponseEntity.ok(ApiResponse.ok("Provider status", health));
    }
}
