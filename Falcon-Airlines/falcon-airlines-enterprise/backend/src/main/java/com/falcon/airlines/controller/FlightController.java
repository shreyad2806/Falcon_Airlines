package com.falcon.airlines.controller;

import com.falcon.airlines.dto.request.FlightRequest;
import com.falcon.airlines.dto.response.FlightResponse;
import com.falcon.airlines.enums.FlightStatus;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.service.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST endpoints for flight management.
 */
@Tag(name = "Flight Management", description = "Flight CRUD, search and scheduling operations")
@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @Operation(summary = "List and search flights")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('FLIGHT_READ')")
    public ResponseEntity<ApiResponse<Page<FlightResponse>>> searchFlights(
            @RequestParam(required = false) String flightNumber,
            @RequestParam(required = false) String originAirport,
            @RequestParam(required = false) String destinationAirport,
            @RequestParam(required = false) String aircraft,
            @RequestParam(required = false) FlightStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant departureFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant departureTo,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        Page<FlightResponse> result = flightService.searchFlights(flightNumber, originAirport, destinationAirport,
                aircraft, status, departureFrom, departureTo, active, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Flights retrieved successfully", result));
    }

    @Operation(summary = "Get flight by ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('FLIGHT_READ')")
    public ResponseEntity<ApiResponse<FlightResponse>> getFlightById(@PathVariable Long id) {
        FlightResponse response = flightService.getFlightById(id);
        return ResponseEntity.ok(ApiResponse.ok("Flight retrieved successfully", response));
    }

    @Operation(summary = "Create a new flight")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('FLIGHT_WRITE')")
    public ResponseEntity<ApiResponse<FlightResponse>> createFlight(@Valid @RequestBody FlightRequest request) {
        FlightResponse response = flightService.createFlight(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Flight created successfully", response));
    }

    @Operation(summary = "Update an existing flight")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('FLIGHT_WRITE')")
    public ResponseEntity<ApiResponse<FlightResponse>> updateFlight(@PathVariable Long id,
                                                                    @Valid @RequestBody FlightRequest request) {
        FlightResponse response = flightService.updateFlight(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Flight updated successfully", response));
    }

    @Operation(summary = "Delete a flight")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('FLIGHT_WRITE')")
    public ResponseEntity<ApiResponse<String>> deleteFlight(@PathVariable Long id) {
        flightService.deleteFlight(id);
        return ResponseEntity.ok(ApiResponse.ok("Flight deleted successfully", "Flight deleted successfully"));
    }
}
