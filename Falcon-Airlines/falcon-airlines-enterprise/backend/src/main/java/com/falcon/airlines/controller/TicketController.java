package com.falcon.airlines.controller;

import com.falcon.airlines.dto.response.TicketDetailResponse;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.enums.TicketStatus;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.service.TicketPdfService;
import com.falcon.airlines.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for ticket management and retrieval.
 */
@Tag(name = "Ticket Management", description = "Ticket retrieval, status management, and generation operations")
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketPdfService ticketPdfService;

    public TicketController(TicketService ticketService, TicketPdfService ticketPdfService) {
        this.ticketService = ticketService;
        this.ticketPdfService = ticketPdfService;
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

    @Operation(summary = "Update ticket status")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('TICKET_WRITE', 'BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> updateTicketStatus(
            @PathVariable Long id,
            @Parameter(description = "New ticket status") @RequestParam TicketStatus status) {
        TicketDetailResponse response = ticketService.updateTicketStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Ticket status updated successfully", response));
    }

    @Operation(summary = "Cancel a ticket")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('TICKET_WRITE', 'BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> cancelTicket(@PathVariable Long id) {
        TicketDetailResponse response = ticketService.cancelTicket(id);
        return ResponseEntity.ok(ApiResponse.ok("Ticket cancelled successfully", response));
    }

    @Operation(summary = "Refund a ticket")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyAuthority('TICKET_WRITE', 'BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> refundTicket(@PathVariable Long id) {
        TicketDetailResponse response = ticketService.refundTicket(id);
        return ResponseEntity.ok(ApiResponse.ok("Ticket refunded successfully", response));
    }

    @Operation(summary = "Mark ticket as used")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/mark-used")
    @PreAuthorize("hasAnyAuthority('TICKET_WRITE', 'BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> markTicketAsUsed(@PathVariable Long id) {
        TicketDetailResponse response = ticketService.markTicketAsUsed(id);
        return ResponseEntity.ok(ApiResponse.ok("Ticket marked as used successfully", response));
    }

    @Operation(summary = "Regenerate ticket for a booking")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/booking/{bookingId}/regenerate")
    @PreAuthorize("hasAnyAuthority('TICKET_WRITE', 'BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> regenerateTicketForBooking(@PathVariable Long bookingId) {
        TicketDetailResponse response = ticketService.regenerateTicketForBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.ok("Ticket regenerated successfully", response));
    }

    @Operation(summary = "Download ticket as PDF")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyAuthority('TICKET_READ', 'BOOKING_READ')")
    public ResponseEntity<byte[]> downloadTicketPdf(@PathVariable Long id) {
        TicketDetailResponse ticketResponse = ticketService.getTicketById(id);
        
        // Convert response to entity for PDF generation
        Ticket ticket = ticketService.getTicketEntityById(id);
        
        byte[] pdfBytes = ticketPdfService.generateTicketPdf(ticket);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "ticket_" + ticket.getTicketNumber() + ".pdf");
        headers.setContentLength(pdfBytes.length);
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
