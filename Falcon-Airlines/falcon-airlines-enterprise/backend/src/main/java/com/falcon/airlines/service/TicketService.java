package com.falcon.airlines.service;

import com.falcon.airlines.dto.response.TicketDetailResponse;
import com.falcon.airlines.entity.Booking;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.entity.Seat;
import com.falcon.airlines.entity.SeatAllocation;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.enums.BookingStatus;
import com.falcon.airlines.enums.TicketStatus;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.repository.BookingRepository;
import com.falcon.airlines.repository.SeatAllocationRepository;
import com.falcon.airlines.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final SeatAllocationRepository seatAllocationRepository;

    public TicketService(TicketRepository ticketRepository,
                        BookingRepository bookingRepository,
                        SeatAllocationRepository seatAllocationRepository) {
        this.ticketRepository = ticketRepository;
        this.bookingRepository = bookingRepository;
        this.seatAllocationRepository = seatAllocationRepository;
    }

    /**
     * Get ticket by ID with authorization check
     * Users can only access tickets for their own bookings
     */
    public TicketDetailResponse getTicketById(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BaseException("Ticket not found", HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND"));

        // Authorization check: user can only access their own tickets
        checkTicketAccessAuthorization(ticket);

        return mapToTicketDetailResponse(ticket);
    }

    /**
     * Get ticket by ticket number with authorization check
     */
    public TicketDetailResponse getTicketByNumber(String ticketNumber) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new BaseException("Ticket not found", HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND"));

        // Authorization check: user can only access their own tickets
        checkTicketAccessAuthorization(ticket);

        return mapToTicketDetailResponse(ticket);
    }

    /**
     * Get all tickets for a booking with authorization check
     */
    public List<TicketDetailResponse> getTicketsByBookingId(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BaseException("Booking not found", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND"));

        // Authorization check: user can only access their own bookings
        checkBookingAccessAuthorization(booking);

        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        return tickets.stream()
                .map(this::mapToTicketDetailResponse)
                .toList();
    }

    /**
     * Get all tickets for a passenger with authorization check
     */
    public List<TicketDetailResponse> getTicketsByPassengerId(Long passengerId) {
        List<Ticket> tickets = ticketRepository.findByPassengerId(passengerId);

        // Authorization check: user can only access tickets for their own bookings
        for (Ticket ticket : tickets) {
            checkTicketAccessAuthorization(ticket);
        }

        return tickets.stream()
                .map(this::mapToTicketDetailResponse)
                .toList();
    }

    /**
     * Regenerate ticket for a booking (only if ticket is VOID or REFUNDED)
     * This prevents duplicate tickets for the same booking unless explicitly allowed
     */
    @Transactional
    public TicketDetailResponse regenerateTicketForBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BaseException("Booking not found", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND"));

        // Authorization check: user can only access their own bookings
        checkBookingAccessAuthorization(booking);

        // Check if booking is confirmed
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BaseException("Booking must be confirmed to generate ticket", 
                    HttpStatus.BAD_REQUEST, "BOOKING_NOT_CONFIRMED");
        }

        // Check if there are existing non-void tickets for this booking
        List<Ticket> existingTickets = ticketRepository.findByBookingId(bookingId);
        boolean hasActiveTickets = existingTickets.stream()
                .anyMatch(t -> t.getStatus() == TicketStatus.ISSUED || t.getStatus() == TicketStatus.ACTIVE);

        if (hasActiveTickets) {
            throw new BaseException("Active tickets already exist for this booking. Cannot regenerate.", 
                    HttpStatus.CONFLICT, "TICKET_ALREADY_EXISTS");
        }

        // Create new tickets for the booking (reuse existing logic from BookingService)
        // For now, this is a placeholder - the actual ticket generation happens during booking creation
        throw new BaseException("Ticket regeneration not yet implemented. Tickets are generated during booking creation.", 
                HttpStatus.NOT_IMPLEMENTED, "TICKET_REGENERATION_NOT_IMPLEMENTED");
    }

    /**
     * Update ticket status with validation
     * 
     * Valid transitions:
     * - ACTIVE → CANCELLED
     * - ACTIVE → REFUNDED
     * - ACTIVE → USED
     * - REFUNDED → CANCELLED
     * 
     * Invalid transitions:
     * - CANCELLED → any other state (terminal state)
     * - USED → any other state (terminal state)
     * - REFUNDED → ACTIVE (cannot reactivate refunded ticket)
     * - REFUNDED → USED (cannot use refunded ticket)
     */
    @Transactional
    public TicketDetailResponse updateTicketStatus(Long ticketId, TicketStatus newStatus) {
        log.info("Updating ticket status: ticketId={}, newStatus={}", ticketId, newStatus);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BaseException("Ticket not found", HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND"));

        // Authorization check: user can only update their own tickets
        checkTicketAccessAuthorization(ticket);

        // Validate status transition
        validateStatusTransition(ticket.getStatus(), newStatus);

        // Update status
        TicketStatus previousStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        Ticket updated = ticketRepository.save(ticket);

        log.info("Ticket status updated successfully: ticketId={}, previousStatus={}, newStatus={}", 
                ticketId, previousStatus, newStatus);

        return mapToTicketDetailResponse(updated);
    }

    /**
     * Cancel a ticket (sets status to CANCELLED)
     * This is a convenience method for the common case of cancelling a ticket
     */
    @Transactional
    public TicketDetailResponse cancelTicket(Long ticketId) {
        log.info("Cancelling ticket: ticketId={}", ticketId);
        return updateTicketStatus(ticketId, TicketStatus.CANCELLED);
    }

    /**
     * Refund a ticket (sets status to REFUNDED)
     * This is a convenience method for the common case of refunding a ticket
     */
    @Transactional
    public TicketDetailResponse refundTicket(Long ticketId) {
        log.info("Refunding ticket: ticketId={}", ticketId);
        return updateTicketStatus(ticketId, TicketStatus.REFUNDED);
    }

    /**
     * Mark a ticket as used (sets status to USED)
     * This is typically called after check-in or flight completion
     */
    @Transactional
    public TicketDetailResponse markTicketAsUsed(Long ticketId) {
        log.info("Marking ticket as used: ticketId={}", ticketId);
        return updateTicketStatus(ticketId, TicketStatus.USED);
    }

    /**
     * Validate that a status transition is allowed according to business rules
     */
    private void validateStatusTransition(TicketStatus currentStatus, TicketStatus newStatus) {
        // Handle legacy states by mapping them to new states
        TicketStatus normalizedCurrent = normalizeLegacyStatus(currentStatus);
        TicketStatus normalizedNew = normalizeLegacyStatus(newStatus);

        // Same status - no change needed
        if (normalizedCurrent == normalizedNew) {
            return;
        }

        // Terminal states cannot transition to any other state
        if (normalizedCurrent == TicketStatus.CANCELLED) {
            throw new BaseException("Cannot change status of cancelled ticket", 
                    HttpStatus.BAD_REQUEST, "TICKET_ALREADY_CANCELLED");
        }

        if (normalizedCurrent == TicketStatus.USED) {
            throw new BaseException("Cannot change status of used ticket", 
                    HttpStatus.BAD_REQUEST, "TICKET_ALREADY_USED");
        }

        // REFUNDED can only transition to CANCELLED
        if (normalizedCurrent == TicketStatus.REFUNDED) {
            if (normalizedNew != TicketStatus.CANCELLED) {
                throw new BaseException("Refunded ticket can only be cancelled, not transitioned to " + newStatus, 
                        HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION");
            }
        }

        // ACTIVE can transition to any state except back to ACTIVE (no-op)
        if (normalizedCurrent == TicketStatus.ACTIVE) {
            // All transitions from ACTIVE are valid
            return;
        }

        // Any other transition is invalid
        throw new BaseException("Invalid status transition from " + currentStatus + " to " + newStatus, 
                HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION");
    }

    /**
     * Normalize legacy status values to their modern equivalents
     * ISSUED → ACTIVE
     * VOID → CANCELLED
     * Other values remain unchanged
     */
    private TicketStatus normalizeLegacyStatus(TicketStatus status) {
        if (status == TicketStatus.ISSUED) {
            return TicketStatus.ACTIVE;
        }
        if (status == TicketStatus.VOID) {
            return TicketStatus.CANCELLED;
        }
        return status;
    }

    /**
     * Check if the current user has access to the ticket
     * Users can access their own tickets, admins can access all
     */
    private void checkTicketAccessAuthorization(Ticket ticket) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BaseException("Authentication required", HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED");
        }

        String username = authentication.getName();
        
        // Check if user is the booking owner
        if (ticket.getBooking() != null && ticket.getBooking().getCustomer() != null) {
            if (ticket.getBooking().getCustomer().getUsername().equals(username)) {
                return; // User is the booking owner
            }
        }

        // Check if user has admin role
        if (authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            return; // Admin can access all tickets
        }

        throw new BaseException("You do not have permission to access this ticket", 
                HttpStatus.FORBIDDEN, "TICKET_ACCESS_DENIED");
    }

    /**
     * Check if the current user has access to the booking
     */
    private void checkBookingAccessAuthorization(Booking booking) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BaseException("Authentication required", HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED");
        }

        String username = authentication.getName();
        
        // Check if user is the booking owner
        if (booking.getCustomer() != null && booking.getCustomer().getUsername().equals(username)) {
            return; // User is the booking owner
        }

        // Check if user has admin role
        if (authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            return; // Admin can access all bookings
        }

        throw new BaseException("You do not have permission to access this booking", 
                HttpStatus.FORBIDDEN, "BOOKING_ACCESS_DENIED");
    }

    /**
     * Map Ticket entity to TicketDetailResponse
     * This includes all travel information required for ticket display
     */
    private TicketDetailResponse mapToTicketDetailResponse(Ticket ticket) {
        TicketDetailResponse response = new TicketDetailResponse();

        // Basic ticket information
        response.setId(ticket.getId());
        response.setTicketNumber(ticket.getTicketNumber());
        response.setStatus(ticket.getStatus());
        response.setIssuedAt(ticket.getIssuedAt());
        response.setVersion(ticket.getVersion());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());

        // Booking information
        if (ticket.getBooking() != null) {
            response.setBookingId(ticket.getBooking().getId());
            response.setBookingReference(ticket.getBooking().getBookingReference());
        }

        // Passenger information
        if (ticket.getPassenger() != null) {
            response.setPassengerId(ticket.getPassenger().getId());
            response.setPassengerName(ticket.getPassenger().getFirstName() + " " + ticket.getPassenger().getLastName());
            response.setPassengerFirstName(ticket.getPassenger().getFirstName());
            response.setPassengerLastName(ticket.getPassenger().getLastName());
        }

        // Flight information
        if (ticket.getFlight() != null) {
            Flight flight = ticket.getFlight();
            response.setFlightId(flight.getId());
            response.setFlightNumber(flight.getFlightNumber());
            response.setAirline("Falcon Airlines"); // Could be made configurable
            response.setScheduledDeparture(flight.getScheduledDeparture());
            response.setScheduledArrival(flight.getScheduledArrival());
            response.setTerminal(flight.getTerminal());
            response.setGate(flight.getGate());

            // Origin airport
            if (flight.getOriginAirport() != null) {
                response.setOriginAirportId(flight.getOriginAirport().getId());
                response.setOriginAirportCode(flight.getOriginAirport().getIataCode());
                response.setOriginAirportName(flight.getOriginAirport().getName());
                response.setOriginCity(flight.getOriginAirport().getCity());
            }

            // Destination airport
            if (flight.getDestinationAirport() != null) {
                response.setDestinationAirportId(flight.getDestinationAirport().getId());
                response.setDestinationAirportCode(flight.getDestinationAirport().getIataCode());
                response.setDestinationAirportName(flight.getDestinationAirport().getName());
                response.setDestinationCity(flight.getDestinationAirport().getCity());
            }
        }

        // Seat information
        SeatAllocation seatAllocation = seatAllocationRepository.findByTicketId(ticket.getId()).orElse(null);
        if (seatAllocation != null && seatAllocation.getSeat() != null) {
            Seat seat = seatAllocation.getSeat();
            response.setSeatId(seat.getId());
            response.setSeatNumber(seat.getSeatNumber());
            response.setSeatClass(seat.getSeatClass());
        }

        // Fare information
        response.setFareBasis(ticket.getFareBasis());
        response.setFare(ticket.getFare());
        response.setTaxes(ticket.getTaxes());
        response.setTotalAmount(ticket.getFare().add(ticket.getTaxes()));
        response.setCurrency("USD"); // Could be made configurable per booking

        // Boarding time (calculated as 30 minutes before departure)
        if (ticket.getFlight() != null && ticket.getFlight().getScheduledDeparture() != null) {
            response.setBoardingTime(ticket.getFlight().getScheduledDeparture().minusSeconds(1800));
        }

        return response;
    }
}
