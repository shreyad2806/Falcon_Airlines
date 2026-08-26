package com.falcon.airlines.controller;

import com.falcon.airlines.dto.request.PaymentRequest;
import com.falcon.airlines.dto.request.SeatHoldRequest;
import com.falcon.airlines.dto.response.PaymentResponse;
import com.falcon.airlines.entity.SeatHold;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.security.principal.UserPrincipal;
import com.falcon.airlines.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment Management", description = "Demo payment processing and seat holds")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Hold a seat temporarily (15 minutes)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/hold-seat")
    @PreAuthorize("hasAnyAuthority('BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<SeatHold>> holdSeat(
            @Valid @RequestBody SeatHoldRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        SeatHold hold = paymentService.holdSeat(
                request.getFlightId(), request.getSeatId(), null, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Seat held successfully", hold));
    }

    @Operation(summary = "Release a seat hold")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/release-seat")
    @PreAuthorize("hasAnyAuthority('BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<String>> releaseSeat(
            @RequestParam Long flightId,
            @RequestParam Long seatId,
            @AuthenticationPrincipal UserPrincipal principal) {
        paymentService.releaseSeatHold(flightId, seatId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Seat released", "Seat hold released successfully"));
    }

    @Operation(summary = "Process demo payment")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/process")
    @PreAuthorize("hasAnyAuthority('BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(ApiResponse.ok("Payment processed successfully", response));
    }

    @Operation(summary = "Simulate payment failure")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/simulate-failure")
    @PreAuthorize("hasAnyAuthority('BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<PaymentResponse>> simulateFailure(
            @RequestParam Long bookingId) {
        PaymentResponse response = paymentService.simulateFailure(bookingId);
        return ResponseEntity.ok(ApiResponse.ok("Payment failure simulated", response));
    }

    @Operation(summary = "Get payment by booking ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyAuthority('BOOKING_READ')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable Long bookingId) {
        PaymentResponse response = paymentService.getPaymentByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.ok("Payment retrieved", response));
    }
}
