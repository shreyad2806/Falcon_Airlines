package com.falcon.airlines.service;

import com.falcon.airlines.dto.response.BoardingPassResponse;
import com.falcon.airlines.entity.Booking;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.entity.Seat;
import com.falcon.airlines.entity.SeatAllocation;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.entity.BoardingPass;
import com.falcon.airlines.enums.BoardingPassStatus;
import com.falcon.airlines.enums.BookingStatus;
import com.falcon.airlines.enums.FlightStatus;
import com.falcon.airlines.enums.TicketStatus;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.repository.BookingRepository;
import com.falcon.airlines.repository.BoardingPassRepository;
import com.falcon.airlines.repository.SeatAllocationRepository;
import com.falcon.airlines.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@Transactional(readOnly = true)
public class BoardingPassService {

    private final BoardingPassRepository boardingPassRepository;
    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final SeatAllocationRepository seatAllocationRepository;

    public BoardingPassService(BoardingPassRepository boardingPassRepository,
                              TicketRepository ticketRepository,
                              BookingRepository bookingRepository,
                              SeatAllocationRepository seatAllocationRepository) {
        this.boardingPassRepository = boardingPassRepository;
        this.ticketRepository = ticketRepository;
        this.bookingRepository = bookingRepository;
        this.seatAllocationRepository = seatAllocationRepository;
    }

    /**
     * Generate boarding pass for a ticket with eligibility validation
     * 
     * Eligibility rules:
     * - Ticket must be ACTIVE
     * - Booking must be CONFIRMED
     * - Flight must be SCHEDULED
     * - No existing boarding pass for this ticket
     */
    @Transactional
    public BoardingPassResponse generateBoardingPass(Long ticketId) {
        log.info("Generating boarding pass for ticket: {}", ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BaseException("Ticket not found", HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND"));

        // Authorization check
        checkTicketAccessAuthorization(ticket);

        // Validate eligibility
        validateBoardingPassEligibility(ticket);

        // Check for existing boarding pass
        List<BoardingPass> existingPasses = boardingPassRepository.findByTicketId(ticketId);
        if (!existingPasses.isEmpty()) {
            throw new BaseException("Boarding pass already exists for this ticket", 
                    HttpStatus.CONFLICT, "BOARDING_PASS_ALREADY_EXISTS");
        }

        // Create boarding pass
        BoardingPass boardingPass = createBoardingPassFromTicket(ticket);
        BoardingPass saved = boardingPassRepository.save(boardingPass);

        log.info("Boarding pass generated successfully: boardingPassNumber={}, ticketId={}", 
                saved.getBoardingPassNumber(), ticketId);

        return mapToBoardingPassResponse(saved);
    }

    /**
     * Get boarding pass by ID with authorization check
     */
    public BoardingPassResponse getBoardingPassById(Long id) {
        BoardingPass boardingPass = boardingPassRepository.findById(id)
                .orElseThrow(() -> new BaseException("Boarding pass not found", HttpStatus.NOT_FOUND, "BOARDING_PASS_NOT_FOUND"));

        // Authorization check
        checkBoardingPassAccessAuthorization(boardingPass);

        return mapToBoardingPassResponse(boardingPass);
    }

    /**
     * Get boarding pass by boarding pass number with authorization check
     */
    public BoardingPassResponse getBoardingPassByNumber(String boardingPassNumber) {
        BoardingPass boardingPass = boardingPassRepository.findByBoardingPassNumber(boardingPassNumber)
                .orElseThrow(() -> new BaseException("Boarding pass not found", HttpStatus.NOT_FOUND, "BOARDING_PASS_NOT_FOUND"));

        // Authorization check
        checkBoardingPassAccessAuthorization(boardingPass);

        return mapToBoardingPassResponse(boardingPass);
    }

    /**
     * Get all boarding passes for a booking with authorization check
     */
    public List<BoardingPassResponse> getBoardingPassesByBookingId(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BaseException("Booking not found", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND"));

        // Authorization check
        checkBookingAccessAuthorization(booking);

        List<BoardingPass> boardingPasses = boardingPassRepository.findByBookingId(bookingId);
        return boardingPasses.stream()
                .map(this::mapToBoardingPassResponse)
                .toList();
    }

    /**
     * Get all boarding passes for a passenger with authorization check
     */
    public List<BoardingPassResponse> getBoardingPassesByPassengerId(Long passengerId) {
        List<BoardingPass> boardingPasses = boardingPassRepository.findByPassengerId(passengerId);

        // Authorization check for each boarding pass
        for (BoardingPass boardingPass : boardingPasses) {
            checkBoardingPassAccessAuthorization(boardingPass);
        }

        return boardingPasses.stream()
                .map(this::mapToBoardingPassResponse)
                .toList();
    }

    /**
     * Update boarding pass status with validation
     */
    @Transactional
    public BoardingPassResponse updateBoardingPassStatus(Long id, BoardingPassStatus newStatus) {
        log.info("Updating boarding pass status: id={}, newStatus={}", id, newStatus);

        BoardingPass boardingPass = boardingPassRepository.findById(id)
                .orElseThrow(() -> new BaseException("Boarding pass not found", HttpStatus.NOT_FOUND, "BOARDING_PASS_NOT_FOUND"));

        // Authorization check
        checkBoardingPassAccessAuthorization(boardingPass);

        // Validate status transition
        validateStatusTransition(boardingPass.getStatus(), newStatus);

        // Update status and timestamps
        BoardingPassStatus previousStatus = boardingPass.getStatus();
        boardingPass.setStatus(newStatus);
        
        if (newStatus == BoardingPassStatus.CHECKED_IN) {
            boardingPass.setCheckedInAt(Instant.now());
        } else if (newStatus == BoardingPassStatus.BOARDING) {
            boardingPass.setBoardedAt(Instant.now());
        } else if (newStatus == BoardingPassStatus.USED) {
            boardingPass.setBoardedAt(Instant.now());
        }

        BoardingPass updated = boardingPassRepository.save(boardingPass);

        log.info("Boarding pass status updated successfully: id={}, previousStatus={}, newStatus={}", 
                id, previousStatus, newStatus);

        return mapToBoardingPassResponse(updated);
    }

    /**
     * Check in passenger (sets status to CHECKED_IN)
     */
    @Transactional
    public BoardingPassResponse checkInBoardingPass(Long id) {
        log.info("Checking in boarding pass: id={}", id);
        return updateBoardingPassStatus(id, BoardingPassStatus.CHECKED_IN);
    }

    /**
     * Mark passenger as boarded (sets status to USED)
     */
    @Transactional
    public BoardingPassResponse boardPassenger(Long id) {
        log.info("Boarding passenger: id={}", id);
        return updateBoardingPassStatus(id, BoardingPassStatus.USED);
    }

    /**
     * Validate boarding pass eligibility
     */
    private void validateBoardingPassEligibility(Ticket ticket) {
        // Check ticket status
        if (ticket.getStatus() != TicketStatus.ACTIVE && ticket.getStatus() != TicketStatus.ISSUED) {
            throw new BaseException("Ticket is not active. Current status: " + ticket.getStatus(), 
                    HttpStatus.BAD_REQUEST, "TICKET_NOT_ACTIVE");
        }

        // Check booking status
        Booking booking = ticket.getBooking();
        if (booking == null || booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BaseException("Booking is not confirmed. Current status: " + 
                    (booking != null ? booking.getStatus() : "null"), 
                    HttpStatus.BAD_REQUEST, "BOOKING_NOT_CONFIRMED");
        }

        // Check flight status
        Flight flight = ticket.getFlight();
        if (flight == null || flight.getStatus() != FlightStatus.SCHEDULED) {
            throw new BaseException("Flight is not scheduled. Current status: " + 
                    (flight != null ? flight.getStatus() : "null"), 
                    HttpStatus.BAD_REQUEST, "FLIGHT_NOT_SCHEDULED");
        }

        // Check if flight is in the future (within 24 hours for check-in)
        if (flight.getScheduledDeparture() != null && flight.getScheduledDeparture().isBefore(Instant.now())) {
            throw new BaseException("Flight has already departed", 
                    HttpStatus.BAD_REQUEST, "FLIGHT_ALREADY_DEPARTED");
        }
    }

    /**
     * Validate status transition for boarding pass
     */
    private void validateStatusTransition(BoardingPassStatus currentStatus, BoardingPassStatus newStatus) {
        // Same status - no change needed
        if (currentStatus == newStatus) {
            return;
        }

        // Terminal states cannot transition to any other state
        if (currentStatus == BoardingPassStatus.VOID) {
            throw new BaseException("Cannot change status of voided boarding pass", 
                    HttpStatus.BAD_REQUEST, "BOARDING_PASS_ALREADY_VOID");
        }

        if (currentStatus == BoardingPassStatus.USED) {
            throw new BaseException("Cannot change status of used boarding pass", 
                    HttpStatus.BAD_REQUEST, "BOARDING_PASS_ALREADY_USED");
        }

        // Valid transitions
        if (currentStatus == BoardingPassStatus.GENERATED) {
            if (newStatus != BoardingPassStatus.CHECKED_IN && newStatus != BoardingPassStatus.VOID) {
                throw new BaseException("Invalid status transition from GENERATED to " + newStatus, 
                        HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION");
            }
        }

        if (currentStatus == BoardingPassStatus.CHECKED_IN) {
            if (newStatus != BoardingPassStatus.BOARDING && newStatus != BoardingPassStatus.VOID) {
                throw new BaseException("Invalid status transition from CHECKED_IN to " + newStatus, 
                        HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION");
            }
        }

        if (currentStatus == BoardingPassStatus.BOARDING) {
            if (newStatus != BoardingPassStatus.USED && newStatus != BoardingPassStatus.VOID) {
                throw new BaseException("Invalid status transition from BOARDING to " + newStatus, 
                        HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION");
            }
        }
    }

    /**
     * Create boarding pass from ticket
     */
    private BoardingPass createBoardingPassFromTicket(Ticket ticket) {
        BoardingPass boardingPass = new BoardingPass();
        boardingPass.setBoardingPassNumber(generateBoardingPassNumber());
        boardingPass.setTicket(ticket);
        boardingPass.setPassenger(ticket.getPassenger());
        boardingPass.setFlight(ticket.getFlight());
        boardingPass.setBooking(ticket.getBooking());
        boardingPass.setStatus(BoardingPassStatus.GENERATED);
        boardingPass.setGeneratedAt(Instant.now());
        boardingPass.setQrCodePayload(generateQrCodePayload(ticket));

        // Get seat information
        SeatAllocation seatAllocation = seatAllocationRepository.findByTicketId(ticket.getId()).orElse(null);
        if (seatAllocation != null && seatAllocation.getSeat() != null) {
            Seat seat = seatAllocation.getSeat();
            boardingPass.setSeatNumber(seat.getSeatNumber());
            boardingPass.setSeatClass(seat.getSeatClass());
        }

        // Get flight information
        Flight flight = ticket.getFlight();
        if (flight != null) {
            boardingPass.setGate(flight.getGate());
            
            // Calculate boarding time (30 minutes before departure)
            if (flight.getScheduledDeparture() != null) {
                boardingPass.setBoardingTime(flight.getScheduledDeparture().minusSeconds(1800));
            }

            // Assign boarding group based on seat class
            if (seatAllocation != null && seatAllocation.getSeat() != null) {
                String seatClass = seatAllocation.getSeat().getSeatClass();
                boardingPass.setBoardingGroup(assignBoardingGroup(seatClass));
            }
        }

        boardingPass.setVersion(0L);
        return boardingPass;
    }

    /**
     * Generate unique boarding pass number
     */
    private String generateBoardingPassNumber() {
        return "BP" + System.currentTimeMillis() + new Random().nextInt(1000);
    }

    /**
     * Generate QR code payload placeholder
     */
    private String generateQrCodePayload(Ticket ticket) {
        // Placeholder for QR code payload
        // In future, this will contain encrypted boarding pass data
        return String.format("BP:%s|TKT:%s|FL:%s|PAX:%s", 
                generateBoardingPassNumber(),
                ticket.getTicketNumber(),
                ticket.getFlight() != null ? ticket.getFlight().getFlightNumber() : "UNKNOWN",
                ticket.getPassenger() != null ? 
                        ticket.getPassenger().getFirstName() + " " + ticket.getPassenger().getLastName() : "UNKNOWN");
    }

    /**
     * Assign boarding group based on seat class
     */
    private String assignBoardingGroup(String seatClass) {
        if (seatClass == null) {
            return "C";
        }
        return switch (seatClass.toUpperCase()) {
            case "FIRST", "BUSINESS" -> "A";
            case "ECONOMY" -> "B";
            default -> "C";
        };
    }

    /**
     * Check if the current user has access to the ticket
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
     * Check if the current user has access to the boarding pass
     */
    private void checkBoardingPassAccessAuthorization(BoardingPass boardingPass) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BaseException("Authentication required", HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED");
        }

        String username = authentication.getName();
        
        // Check if user is the booking owner
        if (boardingPass.getBooking() != null && boardingPass.getBooking().getCustomer() != null) {
            if (boardingPass.getBooking().getCustomer().getUsername().equals(username)) {
                return; // User is the booking owner
            }
        }

        // Check if user has admin role
        if (authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            return; // Admin can access all boarding passes
        }

        throw new BaseException("You do not have permission to access this boarding pass", 
                HttpStatus.FORBIDDEN, "BOARDING_PASS_ACCESS_DENIED");
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
     * Map BoardingPass entity to BoardingPassResponse
     */
    private BoardingPassResponse mapToBoardingPassResponse(BoardingPass boardingPass) {
        BoardingPassResponse response = new BoardingPassResponse();

        // Basic boarding pass information
        response.setId(boardingPass.getId());
        response.setBoardingPassNumber(boardingPass.getBoardingPassNumber());
        response.setStatus(boardingPass.getStatus());
        response.setQrCodePayload(boardingPass.getQrCodePayload());
        response.setGeneratedAt(boardingPass.getGeneratedAt());
        response.setCheckedInAt(boardingPass.getCheckedInAt());
        response.setBoardedAt(boardingPass.getBoardedAt());
        response.setVersion(boardingPass.getVersion());
        response.setCreatedAt(boardingPass.getCreatedAt());
        response.setUpdatedAt(boardingPass.getUpdatedAt());

        // Seat information
        response.setSeatNumber(boardingPass.getSeatNumber());
        response.setSeatClass(boardingPass.getSeatClass());
        response.setBoardingGroup(boardingPass.getBoardingGroup());
        response.setGate(boardingPass.getGate());
        response.setBoardingTime(boardingPass.getBoardingTime());

        // Ticket information
        if (boardingPass.getTicket() != null) {
            response.setTicketId(boardingPass.getTicket().getId());
            response.setTicketNumber(boardingPass.getTicket().getTicketNumber());
        }

        // Passenger information
        if (boardingPass.getPassenger() != null) {
            response.setPassengerId(boardingPass.getPassenger().getId());
            response.setPassengerName(boardingPass.getPassenger().getFirstName() + " " + boardingPass.getPassenger().getLastName());
            response.setPassengerFirstName(boardingPass.getPassenger().getFirstName());
            response.setPassengerLastName(boardingPass.getPassenger().getLastName());
        }

        // Flight information
        if (boardingPass.getFlight() != null) {
            Flight flight = boardingPass.getFlight();
            response.setFlightId(flight.getId());
            response.setFlightNumber(flight.getFlightNumber());
            response.setAirline("Falcon Airlines");
            response.setScheduledDeparture(flight.getScheduledDeparture());
            response.setScheduledArrival(flight.getScheduledArrival());
            response.setTerminal(flight.getTerminal());
            response.setGate(flight.getGate());

            // Origin airport
            if (flight.getOriginAirport() != null) {
                response.setOriginAirportCode(flight.getOriginAirport().getIataCode());
                response.setOriginAirportName(flight.getOriginAirport().getName());
                response.setOriginCity(flight.getOriginAirport().getCity());
            }

            // Destination airport
            if (flight.getDestinationAirport() != null) {
                response.setDestinationAirportCode(flight.getDestinationAirport().getIataCode());
                response.setDestinationAirportName(flight.getDestinationAirport().getName());
                response.setDestinationCity(flight.getDestinationAirport().getCity());
            }
        }

        // Booking information
        if (boardingPass.getBooking() != null) {
            response.setBookingId(boardingPass.getBooking().getId());
            response.setBookingReference(boardingPass.getBooking().getBookingReference());
        }

        return response;
    }
}
