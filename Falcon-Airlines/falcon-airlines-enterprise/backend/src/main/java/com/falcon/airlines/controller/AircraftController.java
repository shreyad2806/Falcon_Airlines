package com.falcon.airlines.controller;

import com.falcon.airlines.dto.request.AircraftRequest;
import com.falcon.airlines.dto.response.AircraftResponse;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.service.AircraftService;
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
 * REST endpoints for aircraft inventory management.
 */
@Tag(name = "Aircraft Management", description = "Aircraft CRUD and search operations")
@RestController
@RequestMapping("/api/aircraft")
public class AircraftController {

    private final AircraftService aircraftService;

    public AircraftController(AircraftService aircraftService) {
        this.aircraftService = aircraftService;
    }

    @Operation(summary = "List and search aircraft")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('FLIGHT_READ')")
    public ResponseEntity<ApiResponse<Page<AircraftResponse>>> searchAircraft(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String registration,
            Pageable pageable) {
        Page<AircraftResponse> result = aircraftService.searchAircraft(type, manufacturer, registration, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Aircraft retrieved successfully", result));
    }

    @Operation(summary = "Get aircraft by ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('FLIGHT_READ')")
    public ResponseEntity<ApiResponse<AircraftResponse>> getAircraftById(@PathVariable Long id) {
        AircraftResponse response = aircraftService.getAircraftById(id);
        return ResponseEntity.ok(ApiResponse.ok("Aircraft retrieved successfully", response));
    }

    @Operation(summary = "Create a new aircraft")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AircraftResponse>> createAircraft(@Valid @RequestBody AircraftRequest request) {
        AircraftResponse response = aircraftService.createAircraft(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Aircraft created successfully", response));
    }

    @Operation(summary = "Update an existing aircraft")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AircraftResponse>> updateAircraft(@PathVariable Long id,
                                                                        @Valid @RequestBody AircraftRequest request) {
        AircraftResponse response = aircraftService.updateAircraft(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Aircraft updated successfully", response));
    }

    @Operation(summary = "Delete an aircraft")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteAircraft(@PathVariable Long id) {
        aircraftService.deleteAircraft(id);
        return ResponseEntity.ok(ApiResponse.ok("Aircraft deleted successfully", "Aircraft deleted successfully"));
    }
}
