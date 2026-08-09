package com.falcon.airlines.controller;

import com.falcon.airlines.dto.request.AirportRequest;
import com.falcon.airlines.dto.response.AirportResponse;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.service.AirportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for airport inventory management.
 */
@Tag(name = "Airport Management", description = "Airport CRUD and search operations")
@RestController
@RequestMapping("/api/airports")
public class AirportController {

    private final AirportService airportService;

    public AirportController(AirportService airportService) {
        this.airportService = airportService;
    }

    @Operation(summary = "List and search airports")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('FLIGHT_READ')")
    public ResponseEntity<ApiResponse<Page<AirportResponse>>> searchAirports(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        Page<AirportResponse> result = airportService.searchAirports(code, name, city, active, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Airports retrieved successfully", result));
    }

    @Operation(summary = "Get airport by ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('FLIGHT_READ')")
    public ResponseEntity<ApiResponse<AirportResponse>> getAirportById(@PathVariable Long id) {
        AirportResponse response = airportService.getAirportById(id);
        return ResponseEntity.ok(ApiResponse.ok("Airport retrieved successfully", response));
    }

    @Operation(summary = "Create a new airport")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AirportResponse>> createAirport(@Valid @RequestBody AirportRequest request) {
        AirportResponse response = airportService.createAirport(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Airport created successfully", response));
    }

    @Operation(summary = "Update an existing airport")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AirportResponse>> updateAirport(@PathVariable Long id,
                                                                      @Valid @RequestBody AirportRequest request) {
        AirportResponse response = airportService.updateAirport(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Airport updated successfully", response));
    }

    @Operation(summary = "Delete an airport")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteAirport(@PathVariable Long id) {
        airportService.deleteAirport(id);
        return ResponseEntity.ok(ApiResponse.ok("Airport deleted successfully", "Airport deleted successfully"));
    }
}
