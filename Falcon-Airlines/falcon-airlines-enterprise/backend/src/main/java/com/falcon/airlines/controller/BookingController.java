package com.falcon.airlines.controller;

import com.falcon.airlines.dto.request.BookingRequest;
import com.falcon.airlines.dto.request.CancelBookingRequest;
import com.falcon.airlines.dto.response.BookingResponse;
import com.falcon.airlines.dto.response.BookingHistoryResponse;
import com.falcon.airlines.dto.response.SeatAvailabilityResponse;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for booking management.
 */
@Tag(name = "Booking Management", description = "Booking creation, modification, cancellation and seat operations")
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Create a new booking")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Booking created successfully", response));
    }

    @Operation(summary = "Get booking by ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('BOOKING_READ')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long id) {
        BookingResponse response = bookingService.getBooking(id);
        return ResponseEntity.ok(ApiResponse.ok("Booking retrieved successfully", response));
    }

    @Operation(summary = "Get booking by reference")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasAnyAuthority('BOOKING_READ')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(@PathVariable String reference) {
        BookingResponse response = bookingService.getBookingByReference(reference);
        return ResponseEntity.ok(ApiResponse.ok("Booking retrieved successfully", response));
    }

    @Operation(summary = "Update an existing booking")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(@PathVariable Long id,
                                                                        @Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.updateBooking(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Booking updated successfully", response));
    }

    @Operation(summary = "Cancel a booking")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<String>> cancelBooking(
            @PathVariable Long id,
            @Valid @RequestBody CancelBookingRequest request) {
        bookingService.cancelBooking(id, request.getCancellationReason());
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled successfully", "Booking cancelled successfully"));
    }

    @Operation(summary = "Get customer booking history")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/history")
    @PreAuthorize("hasAnyAuthority('BOOKING_READ')")
    public ResponseEntity<ApiResponse<BookingHistoryResponse>> getBookingHistory(
            @Parameter(description = "Customer ID") @RequestParam Long customerId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        BookingHistoryResponse response = bookingService.getBookingHistory(customerId, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Booking history retrieved successfully", response));
    }

    @Operation(summary = "Check seat availability for a flight")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/seats/availability")
    @PreAuthorize("hasAnyAuthority('BOOKING_READ')")
    public ResponseEntity<ApiResponse<SeatAvailabilityResponse>> checkSeatAvailability(
            @Parameter(description = "Flight ID") @RequestParam Long flightId) {
        SeatAvailabilityResponse response = bookingService.checkSeatAvailability(flightId);
        return ResponseEntity.ok(ApiResponse.ok("Seat availability retrieved successfully", response));
    }
}
