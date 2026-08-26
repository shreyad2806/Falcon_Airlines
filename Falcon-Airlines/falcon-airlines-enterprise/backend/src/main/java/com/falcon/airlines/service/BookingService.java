package com.falcon.airlines.service;

import com.falcon.airlines.dto.request.BookingRequest;
import com.falcon.airlines.dto.request.SeatAssignmentRequest;
import com.falcon.airlines.dto.response.BookingResponse;
import com.falcon.airlines.dto.response.BookingHistoryResponse;
import com.falcon.airlines.dto.response.BookingSummaryResponse;
import com.falcon.airlines.dto.response.SeatAvailabilityResponse;
import com.falcon.airlines.entity.Booking;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.entity.Seat;
import com.falcon.airlines.entity.SeatAllocation;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.enums.BookingPaymentStatus;
import com.falcon.airlines.enums.BookingStatus;
import com.falcon.airlines.enums.FlightStatus;
import com.falcon.airlines.enums.TicketStatus;
import com.falcon.airlines.exception.BaseException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import com.falcon.airlines.repository.BookingRepository;
import com.falcon.airlines.repository.FlightRepository;
import com.falcon.airlines.repository.PassengerRepository;
import com.falcon.airlines.repository.SeatAllocationRepository;
import com.falcon.airlines.repository.SeatRepository;
import com.falcon.airlines.repository.TicketRepository;
import com.falcon.airlines.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final PassengerRepository passengerRepository;
    private final FlightRepository flightRepository;
    private final SeatRepository seatRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          TicketRepository ticketRepository,
                          PassengerRepository passengerRepository,
                          FlightRepository flightRepository,
                          SeatRepository seatRepository,
                          SeatAllocationRepository seatAllocationRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.passengerRepository = passengerRepository;
        this.flightRepository = flightRepository;
        this.seatRepository = seatRepository;
        this.seatAllocationRepository = seatAllocationRepository;
        this.userRepository = userRepository;
    }

    public BookingResponse createBooking(BookingRequest request) {
        log.info("Creating booking for customer: {}, flight: {}", request.getCustomerId(), request.getFlightId());

        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new BaseException("Customer not found", HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND"));

        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new BaseException("Flight not found", HttpStatus.NOT_FOUND, "FLIGHT_NOT_FOUND"));

        validateFlightIsBookable(flight);

        Set<String> uniqueSeats = new HashSet<>(request.getRequestedSeats());
        if (uniqueSeats.size() != request.getRequestedSeats().size()) {
            throw new BaseException("Duplicate seats in request", HttpStatus.BAD_REQUEST, "DUPLICATE_SEATS_IN_REQUEST");
        }

        if (uniqueSeats.size() != request.getPassengers().size()) {
            throw new BaseException("Number of seats must match number of passengers", HttpStatus.BAD_REQUEST, "SEAT_PASSENGER_MISMATCH");
        }

        validatePassengersExist(request.getPassengers());

        List<Seat> seats = validateAndResolveSeats(flight.getAircraft().getId(), uniqueSeats);

        validateSeatsAvailable(flight.getId(), seats);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setBookingDate(Instant.now());
        booking.setPaymentStatus(BookingPaymentStatus.PENDING);
        booking.setBookingReference(generateBookingReference());
        booking.setCurrency("INR");
        booking.setTotalAmount(calculateTotalAmountInINR(seats, flight));

        Booking savedBooking = bookingRepository.save(booking);

        List<Ticket> tickets = new ArrayList<>();
        List<BookingRequest.BookingPassengerRequest> passengerRequests = request.getPassengers();

        int index = 0;
        for (Seat seat : seats) {
            Passenger passenger = passengerRepository.findById(passengerRequests.get(index).getPassengerId())
                    .orElseThrow(() -> new BaseException("Passenger not found", HttpStatus.NOT_FOUND, "PASSENGER_NOT_FOUND"));

            Ticket ticket = new Ticket();
            ticket.setBooking(savedBooking);
            ticket.setPassenger(passenger);
            ticket.setFlight(flight);
            ticket.setTicketNumber(generateTicketNumber());
            ticket.setFareBasis(passengerRequests.get(index).getFareClass() != null ? passengerRequests.get(index).getFareClass() : "ECONOMY");
            ticket.setFare(calculateSeatFare(seat));
            ticket.setTaxes(calculateSeatTaxes(seat));
            ticket.setStatus(TicketStatus.ACTIVE);
            ticket.setIssuedAt(Instant.now());

            Ticket savedTicket = ticketRepository.save(ticket);
            tickets.add(savedTicket);

            SeatAllocation allocation = new SeatAllocation();
            allocation.setSeat(seat);
            allocation.setTicket(savedTicket);
            allocation.setFlight(flight);
            allocation.setAllocatedAt(Instant.now());

            try {
                seatAllocationRepository.save(allocation);
            } catch (DataIntegrityViolationException e) {
                throw new BaseException("Seat " + seat.getSeatNumber() + " is already allocated on this flight", 
                        HttpStatus.CONFLICT, "SEAT_ALREADY_ALLOCATED");
            }

            index++;
        }

        log.info("Booking created successfully: {}, reference: {}", savedBooking.getId(), savedBooking.getBookingReference());

        return mapToBookingResponse(savedBooking, tickets);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long id) {
        log.info("Fetching booking: {}", id);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BaseException("Booking not found", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND"));

        List<Ticket> tickets = ticketRepository.findByBookingId(id);

        return mapToBookingResponse(booking, tickets);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String reference) {
        log.info("Fetching booking by reference: {}", reference);

        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new BaseException("Booking not found", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND"));

        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());

        return mapToBookingResponse(booking, tickets);
    }

    public BookingResponse updateBooking(Long id, BookingRequest request) {
        log.info("Updating booking: {}", id);

        Booking existing = bookingRepository.findById(id)
                .orElseThrow(() -> new BaseException("Booking not found", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND"));

        if (existing.getStatus() == BookingStatus.CANCELLED || existing.getStatus() == BookingStatus.COMPLETED) {
            throw new BaseException("Cannot update cancelled or completed booking", HttpStatus.BAD_REQUEST, "BOOKING_NOT_UPDATABLE");
        }

        if (request.getCustomerId() != null && !request.getCustomerId().equals(existing.getCustomer().getId())) {
            throw new BaseException("Cannot change customer on existing booking", HttpStatus.BAD_REQUEST, "CUSTOMER_CHANGE_NOT_ALLOWED");
        }

        if (existing.getStatus() == BookingStatus.CONFIRMED) {
            throw new BaseException("Cannot modify confirmed booking. Contact support for changes.", HttpStatus.BAD_REQUEST, "CONFIRMED_BOOKING_IMMUTABLE");
        }

        Booking updated = bookingRepository.save(existing);

        log.info("Booking updated successfully: {}", id);

        List<Ticket> tickets = ticketRepository.findByBookingId(id);
        return mapToBookingResponse(updated, tickets);
    }

    public void cancelBooking(Long id, String cancellationReason) {
        log.info("Cancelling booking: {}, reason: {}", id, cancellationReason);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BaseException("Booking not found", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BaseException("Booking is already cancelled", HttpStatus.BAD_REQUEST, "BOOKING_ALREADY_CANCELLED");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BaseException("Cannot cancel completed booking", HttpStatus.BAD_REQUEST, "BOOKING_ALREADY_COMPLETED");
        }

        BookingStatus previousStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        List<Ticket> tickets = ticketRepository.findByBookingId(id);
        for (Ticket ticket : tickets) {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);

            seatAllocationRepository.findByTicketId(ticket.getId()).ifPresent(allocation -> {
                seatAllocationRepository.delete(allocation);
                log.info("Released seat allocation: {} for ticket: {}", allocation.getId(), ticket.getId());
            });
        }

        log.info("Booking cancelled successfully: {}, previous status: {}", id, previousStatus);
    }

    @Transactional(readOnly = true)
    public BookingHistoryResponse getBookingHistory(Long customerId, int page, int size) {
        log.info("Fetching booking history for customer: {}", customerId);

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new BaseException("Customer not found", HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Booking> bookingsPage = bookingRepository.findByCustomerId(customerId, pageable);

        List<BookingSummaryResponse> summaries = bookingsPage.getContent().stream()
                .map(this::mapToBookingSummary)
                .collect(Collectors.toList());

        BookingHistoryResponse response = new BookingHistoryResponse();
        response.setCustomerId(customerId);
        response.setCustomerUsername(customer.getUsername());
        response.setTotalBookings((int) bookingsPage.getTotalElements());
        response.setBookings(summaries);

        return response;
    }

    @Transactional(readOnly = true)
    public SeatAvailabilityResponse checkSeatAvailability(Long flightId) {
        log.info("Checking seat availability for flight: {}", flightId);

        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new BaseException("Flight not found", HttpStatus.NOT_FOUND, "FLIGHT_NOT_FOUND"));

        List<Seat> allSeats = seatRepository.findByAircraftIdAndIsActiveTrue(flight.getAircraft().getId());
        List<Seat> availableSeats = seatRepository.findAvailableSeatsForFlight(flight.getAircraft().getId(), flightId);

        Set<Long> availableSeatIds = availableSeats.stream().map(Seat::getId).collect(Collectors.toSet());

        int totalSeats = allSeats.size();
        int available = availableSeats.size();
        int blocked = totalSeats - available;

        List<SeatAvailabilityResponse.SeatDetailResponse> seatDetails = allSeats.stream()
                .map(seat -> {
                    SeatAvailabilityResponse.SeatDetailResponse detail = new SeatAvailabilityResponse.SeatDetailResponse();
                    detail.setSeatId(seat.getId());
                    detail.setSeatNumber(seat.getSeatNumber());
                    detail.setSeatClass(seat.getSeatClass());
                    detail.setSeatType(seat.getSeatType() != null ? seat.getSeatType() : "STANDARD");
                    detail.setRowNumber(seat.getRowNumber());
                    detail.setColumnLetter(seat.getColumnLetter());
                    detail.setPrice(seat.getPrice() != null ? seat.getPrice() : java.math.BigDecimal.ZERO);
                    if (!seat.getIsActive()) {
                        detail.setIsAvailable(false);
                        detail.setStatus("BLOCKED");
                    } else if (availableSeatIds.contains(seat.getId())) {
                        detail.setIsAvailable(true);
                        detail.setStatus("AVAILABLE");
                    } else {
                        detail.setIsAvailable(false);
                        detail.setStatus("OCCUPIED");
                    }
                    return detail;
                })
                .collect(Collectors.toList());

        SeatAvailabilityResponse response = new SeatAvailabilityResponse();
        response.setFlightId(flightId);
        response.setFlightNumber(flight.getFlightNumber());
        response.setAircraftId(flight.getAircraft().getId());
        response.setAircraftRegistrationNumber(flight.getAircraft().getRegistrationNumber());
        response.setAircraftType(flight.getAircraft().getType());
        response.setTotalSeats(totalSeats);
        response.setAvailableSeats(available);
        response.setBookedSeats(blocked);
        response.setBlockedSeats(0);
        response.setSeats(seatDetails);

        return response;
    }

    public void assignSeat(SeatAssignmentRequest request) {
        log.info("Assigning seat: {} to ticket: {} for flight: {}", request.getSeatNumber(), request.getTicketId(), request.getFlightId());

        Ticket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new BaseException("Ticket not found", HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND"));

        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new BaseException("Flight not found", HttpStatus.NOT_FOUND, "FLIGHT_NOT_FOUND"));

        if (ticket.getStatus() == TicketStatus.VOID || ticket.getStatus() == TicketStatus.REFUNDED) {
            throw new BaseException("Cannot assign seat to void or refunded ticket", HttpStatus.BAD_REQUEST, "TICKET_INVALID");
        }

        seatAllocationRepository.findByTicketId(request.getTicketId()).ifPresent(allocation -> {
            throw new BaseException("Ticket already has a seat assigned", HttpStatus.CONFLICT, "SEAT_ALREADY_ASSIGNED");
        });

        Seat seat = seatRepository.findByAircraftIdAndSeatNumber(flight.getAircraft().getId(), request.getSeatNumber())
                .orElseThrow(() -> new BaseException("Seat not found on this aircraft", HttpStatus.NOT_FOUND, "SEAT_NOT_FOUND"));

        if (!seat.getIsActive()) {
            throw new BaseException("Seat is not active", HttpStatus.BAD_REQUEST, "SEAT_INACTIVE");
        }

        boolean isAllocated = seatAllocationRepository.existsBySeatIdAndFlightId(seat.getId(), request.getFlightId());
        if (isAllocated) {
            throw new BaseException("Seat is already allocated for this flight", HttpStatus.CONFLICT, "SEAT_ALREADY_ALLOCATED");
        }

        try {
            SeatAllocation allocation = new SeatAllocation();
            allocation.setSeat(seat);
            allocation.setTicket(ticket);
            allocation.setFlight(flight);
            allocation.setAllocatedAt(Instant.now());

            seatAllocationRepository.save(allocation);

            log.info("Seat assigned successfully: {} to ticket: {}", seat.getSeatNumber(), ticket.getTicketNumber());
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Concurrent seat allocation conflict for seat: {}, flight: {}", seat.getSeatNumber(), flight.getFlightNumber());
            throw new BaseException("Seat was allocated by another transaction. Please try again.", HttpStatus.CONFLICT, "CONCURRENT_ALLOCATION", e);
        }
    }

    public void releaseSeat(Long ticketId) {
        log.info("Releasing seat for ticket: {}", ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BaseException("Ticket not found", HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND"));

        SeatAllocation allocation = seatAllocationRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new BaseException("No seat allocation found for this ticket", HttpStatus.NOT_FOUND, "SEAT_ALLOCATION_NOT_FOUND"));

        seatAllocationRepository.delete(allocation);

        log.info("Seat released successfully for ticket: {}", ticketId);
    }

    private void validateFlightIsBookable(Flight flight) {
        if (flight.getStatus() == FlightStatus.CANCELLED) {
            throw new BaseException("Flight is cancelled", HttpStatus.BAD_REQUEST, "FLIGHT_CANCELLED");
        }

        if (!flight.getIsActive()) {
            throw new BaseException("Flight is not active for booking", HttpStatus.BAD_REQUEST, "FLIGHT_INACTIVE");
        }

        if (flight.getScheduledDeparture().isBefore(Instant.now())) {
            throw new BaseException("This flight has already departed. Please choose another flight.", HttpStatus.BAD_REQUEST, "FLIGHT_IN_PAST");
        }
    }

    private void validatePassengersExist(List<BookingRequest.BookingPassengerRequest> passengerRequests) {
        for (BookingRequest.BookingPassengerRequest passengerRequest : passengerRequests) {
            if (!passengerRepository.existsById(passengerRequest.getPassengerId())) {
                throw new BaseException("Passenger not found: " + passengerRequest.getPassengerId(), HttpStatus.NOT_FOUND, "PASSENGER_NOT_FOUND");
            }
        }
    }

    private List<Seat> validateAndResolveSeats(Long aircraftId, Set<String> seatNumbers) {
        List<Seat> seats = new ArrayList<>();
        for (String seatNumber : seatNumbers) {
            Seat seat = seatRepository.findByAircraftIdAndSeatNumber(aircraftId, seatNumber)
                    .orElseThrow(() -> new BaseException("Seat not found on aircraft: " + seatNumber, HttpStatus.NOT_FOUND, "SEAT_NOT_FOUND"));

            if (!seat.getIsActive()) {
                throw new BaseException("Seat is not active: " + seatNumber, HttpStatus.BAD_REQUEST, "SEAT_INACTIVE");
            }

            seats.add(seat);
        }
        return seats;
    }

    private void validateSeatsAvailable(Long flightId, List<Seat> seats) {
        for (Seat seat : seats) {
            boolean isAllocated = seatAllocationRepository.existsBySeatIdAndFlightId(seat.getId(), flightId);
            if (isAllocated) {
                throw new BaseException("Seat already allocated: " + seat.getSeatNumber(), HttpStatus.CONFLICT, "SEAT_ALREADY_ALLOCATED");
            }
        }
    }

    private String generateBookingReference() {
        return "BK" + String.format("%08d", (int)(Math.random() * 100000000));
    }

    private String generateTicketNumber() {
        return "TKT" + System.currentTimeMillis() + (int)(Math.random() * 10000);
    }

    private BigDecimal calculateTotalAmountInINR(List<Seat> seats, Flight flight) {
        BigDecimal basePrice = flight.getBasePrice() != null ? flight.getBasePrice() : BigDecimal.valueOf(35000);
        int passengerCount = seats.size();
        BigDecimal totalFare = basePrice.multiply(BigDecimal.valueOf(passengerCount));
        BigDecimal taxes = totalFare.multiply(BigDecimal.valueOf(0.12)); // 12% GST
        return totalFare.add(taxes);
    }

    private BigDecimal calculateSeatFare(Seat seat) {
        return switch (seat.getSeatClass()) {
            case "FIRST" -> BigDecimal.valueOf(500.00);
            case "BUSINESS" -> BigDecimal.valueOf(300.00);
            case "ECONOMY" -> BigDecimal.valueOf(100.00);
            default -> BigDecimal.valueOf(100.00);
        };
    }

    private BigDecimal calculateSeatTaxes(Seat seat) {
        return calculateSeatFare(seat).multiply(BigDecimal.valueOf(0.20));
    }

    private BookingResponse mapToBookingResponse(Booking booking, List<Ticket> tickets) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setBookingReference(booking.getBookingReference());
        response.setCustomerId(booking.getCustomer().getId());
        response.setCustomerUsername(booking.getCustomer().getUsername());
        
        if (!tickets.isEmpty()) {
            response.setFlightId(tickets.get(0).getFlight().getId());
            response.setFlightNumber(tickets.get(0).getFlight().getFlightNumber());
        }
        response.setStatus(booking.getStatus());
        response.setTotalAmount(booking.getTotalAmount());
        response.setCurrency(booking.getCurrency());
        response.setBookingDate(booking.getBookingDate());
        response.setTimeLimit(booking.getTimeLimit());
        response.setPaymentStatus(booking.getPaymentStatus());
        response.setVersion(booking.getVersion());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());

        List<com.falcon.airlines.dto.response.TicketSummaryResponse> ticketSummaries = tickets.stream()
                .map(this::mapToTicketSummary)
                .collect(Collectors.toList());
        response.setTickets(ticketSummaries);

        return response;
    }

    private com.falcon.airlines.dto.response.TicketSummaryResponse mapToTicketSummary(Ticket ticket) {
        com.falcon.airlines.dto.response.TicketSummaryResponse summary = new com.falcon.airlines.dto.response.TicketSummaryResponse();
        summary.setId(ticket.getId());
        summary.setTicketNumber(ticket.getTicketNumber());
        summary.setPassengerId(ticket.getPassenger().getId());
        summary.setPassengerName(ticket.getPassenger().getFirstName() + " " + ticket.getPassenger().getLastName());
        summary.setFlightId(ticket.getFlight().getId());
        summary.setFlightNumber(ticket.getFlight().getFlightNumber());
        summary.setFareBasis(ticket.getFareBasis());
        summary.setFare(ticket.getFare());
        summary.setTaxes(ticket.getTaxes());
        summary.setStatus(ticket.getStatus());
        summary.setIssuedAt(ticket.getIssuedAt());

        seatAllocationRepository.findByTicketId(ticket.getId()).ifPresent(allocation -> {
            summary.setSeatNumber(allocation.getSeat().getSeatNumber());
            summary.setSeatClass(allocation.getSeat().getSeatClass());
        });

        return summary;
    }

    private BookingSummaryResponse mapToBookingSummary(Booking booking) {
        BookingSummaryResponse summary = new BookingSummaryResponse();
        summary.setId(booking.getId());
        summary.setBookingReference(booking.getBookingReference());
        summary.setCustomerId(booking.getCustomer().getId());
        summary.setStatus(booking.getStatus());
        summary.setTotalAmount(booking.getTotalAmount());
        summary.setCurrency(booking.getCurrency());
        summary.setBookingDate(booking.getBookingDate());
        summary.setTimeLimit(booking.getTimeLimit());
        summary.setPaymentStatus(booking.getPaymentStatus());
        summary.setCreatedAt(booking.getCreatedAt());

        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        summary.setPassengerCount(tickets.size());
        
        if (!tickets.isEmpty()) {
            summary.setFlightId(tickets.get(0).getFlight().getId());
            summary.setFlightNumber(tickets.get(0).getFlight().getFlightNumber());
        }

        return summary;
    }
}
