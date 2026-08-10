package com.falcon.airlines.controller;

import com.falcon.airlines.dto.request.PassengerRequest;
import com.falcon.airlines.dto.response.PassengerResponse;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.service.PassengerService;
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
 * REST endpoints for passenger management.
 */
@Tag(name = "Passenger Management", description = "Passenger CRUD, search and history operations")
@RestController
@RequestMapping("/api/passengers")
public class PassengerController {

    private final PassengerService passengerService;

    public PassengerController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }

    @Operation(summary = "List and search passengers")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('PASSENGER_READ')")
    public ResponseEntity<ApiResponse<Page<PassengerResponse>>> searchPassengers(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String passportNumber,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String fullName,
            Pageable pageable) {
        Page<PassengerResponse> result = passengerService.searchPassengers(firstName, lastName, email, 
                passportNumber, userId, fullName, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Passengers retrieved successfully", result));
    }

    @Operation(summary = "Get passenger by ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PASSENGER_READ')")
    public ResponseEntity<ApiResponse<PassengerResponse>> getPassengerById(@PathVariable Long id) {
        PassengerResponse response = passengerService.getPassengerById(id);
        return ResponseEntity.ok(ApiResponse.ok("Passenger retrieved successfully", response));
    }

    @Operation(summary = "Get passenger history")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyAuthority('PASSENGER_READ')")
    public ResponseEntity<ApiResponse<PassengerResponse>> getPassengerHistory(@PathVariable Long id) {
        PassengerResponse response = passengerService.getPassengerHistory(id);
        return ResponseEntity.ok(ApiResponse.ok("Passenger history retrieved successfully", response));
    }

    @Operation(summary = "Create a new passenger")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('PASSENGER_WRITE')")
    public ResponseEntity<ApiResponse<PassengerResponse>> createPassenger(@Valid @RequestBody PassengerRequest request) {
        PassengerResponse response = passengerService.createPassenger(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Passenger created successfully", response));
    }

    @Operation(summary = "Update an existing passenger")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PASSENGER_WRITE')")
    public ResponseEntity<ApiResponse<PassengerResponse>> updatePassenger(@PathVariable Long id,
                                                                        @Valid @RequestBody PassengerRequest request) {
        PassengerResponse response = passengerService.updatePassenger(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Passenger updated successfully", response));
    }

    @Operation(summary = "Delete a passenger")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PASSENGER_WRITE')")
    public ResponseEntity<ApiResponse<String>> deletePassenger(@PathVariable Long id) {
        passengerService.deletePassenger(id);
        return ResponseEntity.ok(ApiResponse.ok("Passenger deleted successfully", "Passenger deleted successfully"));
    }
}
