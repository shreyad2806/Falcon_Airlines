package com.falcon.airlines.controller;

import com.falcon.airlines.dto.response.TicketDetailResponse;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for ticket management and retrieval.
 */
@Tag(name = "Ticket Management", description = "Ticket retrieval and generation operations")
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Get ticket by ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TICKET_READ', 'BOOKING_READ')")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getTicketById(@PathVariable Long id) {
        TicketDetailResponse response = ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.ok("Ticket retrieved successfully", response));
    }

    @Operation(summary = "Get ticket by ticket number")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/number/{ticketNumber}")
    @PreAuthorize("hasAnyAuthority('TICKET_READ', 'BOOKING_READ')")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getTicketByNumber(@PathVariable String ticketNumber) {
        TicketDetailResponse response = ticketService.getTicketByNumber(ticketNumber);
        return ResponseEntity.ok(ApiResponse.ok("Ticket retrieved successfully", response));
    }

    @Operation(summary = "Get all tickets for a booking")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyAuthority('TICKET_READ', 'BOOKING_READ')")
    public ResponseEntity<ApiResponse<List<TicketDetailResponse>>> getTicketsByBookingId(@PathVariable Long bookingId) {
        List<TicketDetailResponse> response = ticketService.getTicketsByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.ok("Tickets retrieved successfully", response));
    }

    @Operation(summary = "Get all tickets for a passenger")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/passenger/{passengerId}")
    @PreAuthorize("hasAnyAuthority('TICKET_READ', 'BOOKING_READ')")
    public ResponseEntity<ApiResponse<List<TicketDetailResponse>>> getTicketsByPassengerId(@PathVariable Long passengerId) {
        List<TicketDetailResponse> response = ticketService.getTicketsByPassengerId(passengerId);
        return ResponseEntity.ok(ApiResponse.ok("Tickets retrieved successfully", response));
    }

    @Operation(summary = "Regenerate ticket for a booking")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/booking/{bookingId}/regenerate")
    @PreAuthorize("hasAnyAuthority('TICKET_WRITE', 'BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> regenerateTicketForBooking(@PathVariable Long bookingId) {
        TicketDetailResponse response = ticketService.regenerateTicketForBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.ok("Ticket regenerated successfully", response));
    }
}
