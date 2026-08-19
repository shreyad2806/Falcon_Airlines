package com.falcon.airlines.service;

import com.falcon.airlines.common.BaseUnitTest;
import com.falcon.airlines.dto.response.TicketDetailResponse;
import com.falcon.airlines.entity.Aircraft;
import com.falcon.airlines.entity.Airport;
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
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TicketServiceTest extends BaseUnitTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatAllocationRepository seatAllocationRepository;

    @InjectMocks
    private TicketService ticketService;

    private User buildUser(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        return user;
    }

    private Booking buildBooking(Long id, User customer) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setBookingReference("BK123456");
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(BigDecimal.valueOf(200.00));
        booking.setCurrency("USD");
        booking.setBookingDate(Instant.now());
        booking.setCustomer(customer);
        booking.setVersion(0L);
        return booking;
    }

    private Passenger buildPassenger(Long id) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        passenger.setFirstName("John");
        passenger.setLastName("Doe");
        return passenger;
    }

    private Airport buildAirport(String code, String city) {
        Airport airport = new Airport();
        airport.setId(1L);
        airport.setIataCode(code);
        airport.setName(code + " Airport");
        airport.setCity(city);
        return airport;
    }

    private Flight buildFlight(Long id) {
        Flight flight = new Flight();
        flight.setId(id);
        flight.setFlightNumber("FL001");
        flight.setScheduledDeparture(Instant.now().plusSeconds(3600));
        flight.setScheduledArrival(Instant.now().plusSeconds(7200));
        flight.setTerminal("T1");
        flight.setGate("A1");
        flight.setOriginAirport(buildAirport("JFK", "New York"));
        flight.setDestinationAirport(buildAirport("LAX", "Los Angeles"));
        return flight;
    }

    private Seat buildSeat(Long id, String seatNumber) {
        Seat seat = new Seat();
        seat.setId(id);
        seat.setSeatNumber(seatNumber);
        seat.setSeatClass("ECONOMY");
        return seat;
    }

    private SeatAllocation buildSeatAllocation(Long id, Seat seat) {
        SeatAllocation allocation = new SeatAllocation();
        allocation.setId(id);
        allocation.setSeat(seat);
        allocation.setAllocatedAt(Instant.now());
        return allocation;
    }

    private Ticket buildTicket(Long id, Booking booking, Passenger passenger, Flight flight) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTicketNumber("TKT123456");
        ticket.setStatus(TicketStatus.ISSUED);
        ticket.setFare(BigDecimal.valueOf(100.00));
        ticket.setTaxes(BigDecimal.valueOf(20.00));
        ticket.setFareBasis("ECONOMY");
        ticket.setIssuedAt(Instant.now());
        ticket.setBooking(booking);
        ticket.setPassenger(passenger);
        ticket.setFlight(flight);
        ticket.setVersion(0L);
        return ticket;
    }

    private void setupSecurityContext(String username, String role) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(username);
        lenient().when(authentication.getAuthorities()).thenReturn((List) List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        lenient().when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTicketById_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        Seat seat = buildSeat(1L, "1A");
        SeatAllocation allocation = buildSeatAllocation(1L, seat);
        allocation.setTicket(ticket);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.of(allocation));

        TicketDetailResponse response = ticketService.getTicketById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTicketNumber()).isEqualTo("TKT123456");
        assertThat(response.getBookingId()).isEqualTo(1L);
        assertThat(response.getBookingReference()).isEqualTo("BK123456");
        assertThat(response.getPassengerName()).isEqualTo("John Doe");
        assertThat(response.getFlightNumber()).isEqualTo("FL001");
        assertThat(response.getOriginAirportCode()).isEqualTo("JFK");
        assertThat(response.getDestinationAirportCode()).isEqualTo("LAX");
        assertThat(response.getSeatNumber()).isEqualTo("1A");
        assertThat(response.getFare()).isEqualTo(BigDecimal.valueOf(100.00));
        assertThat(response.getTaxes()).isEqualTo(BigDecimal.valueOf(20.00));
        assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.valueOf(120.00));
        assertThat(response.getStatus()).isEqualTo(TicketStatus.ISSUED);

        clearSecurityContext();
    }

    @Test
    void getTicketById_notFound() {
        setupSecurityContext("customer", "CUSTOMER");
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_NOT_FOUND");

        clearSecurityContext();
    }

    @Test
    void getTicketById_unauthorized() {
        setupSecurityContext("otheruser", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.getTicketById(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_ACCESS_DENIED");

        clearSecurityContext();
    }

    @Test
    void getTicketById_adminCanAccessAll() {
        setupSecurityContext("admin", "ADMIN");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        Seat seat = buildSeat(1L, "1A");
        SeatAllocation allocation = buildSeatAllocation(1L, seat);
        allocation.setTicket(ticket);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.of(allocation));

        TicketDetailResponse response = ticketService.getTicketById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);

        clearSecurityContext();
    }

    @Test
    void getTicketByNumber_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);

        when(ticketRepository.findByTicketNumber("TKT123456")).thenReturn(Optional.of(ticket));
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

        TicketDetailResponse response = ticketService.getTicketByNumber("TKT123456");

        assertThat(response).isNotNull();
        assertThat(response.getTicketNumber()).isEqualTo("TKT123456");

        clearSecurityContext();
    }

    @Test
    void getTicketByNumber_notFound() {
        setupSecurityContext("customer", "CUSTOMER");
        when(ticketRepository.findByTicketNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketByNumber("INVALID"))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_NOT_FOUND");

        clearSecurityContext();
    }

    @Test
    void getTicketsByBookingId_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket1 = buildTicket(1L, booking, passenger, flight);
        Ticket ticket2 = buildTicket(2L, booking, passenger, flight);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(ticketRepository.findByBookingId(1L)).thenReturn(List.of(ticket1, ticket2));
        when(seatAllocationRepository.findByTicketId(any())).thenReturn(Optional.empty());

        List<TicketDetailResponse> response = ticketService.getTicketsByBookingId(1L);

        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getBookingId()).isEqualTo(1L);
        assertThat(response.get(1).getBookingId()).isEqualTo(1L);

        clearSecurityContext();
    }

    @Test
    void getTicketsByBookingId_bookingNotFound() {
        setupSecurityContext("customer", "CUSTOMER");
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketsByBookingId(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "BOOKING_NOT_FOUND");

        clearSecurityContext();
    }

    @Test
    void getTicketsByBookingId_unauthorized() {
        setupSecurityContext("otheruser", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> ticketService.getTicketsByBookingId(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "BOOKING_ACCESS_DENIED");

        clearSecurityContext();
    }

    @Test
    void getTicketsByPassengerId_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);

        when(ticketRepository.findByPassengerId(1L)).thenReturn(List.of(ticket));
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

        List<TicketDetailResponse> response = ticketService.getTicketsByPassengerId(1L);

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getPassengerId()).isEqualTo(1L);

        clearSecurityContext();
    }

    @Test
    void regenerateTicketForBooking_bookingNotFound() {
        setupSecurityContext("customer", "CUSTOMER");
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.regenerateTicketForBooking(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "BOOKING_NOT_FOUND");

        clearSecurityContext();
    }

    @Test
    void regenerateTicketForBooking_bookingNotConfirmed() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> ticketService.regenerateTicketForBooking(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "BOOKING_NOT_CONFIRMED");

        clearSecurityContext();
    }

    @Test
    void regenerateTicketForBooking_activeTicketsExist() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(ticketRepository.findByBookingId(1L)).thenReturn(List.of(ticket));

        assertThatThrownBy(() -> ticketService.regenerateTicketForBooking(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_ALREADY_EXISTS");

        clearSecurityContext();
    }

    @Test
    void regenerateTicketForBooking_notImplemented() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(ticketRepository.findByBookingId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> ticketService.regenerateTicketForBooking(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_IMPLEMENTED)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_REGENERATION_NOT_IMPLEMENTED");

        clearSecurityContext();
    }

    @Test
    void getTicketById_unauthenticated() {
        SecurityContextHolder.clearContext();

        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.getTicketById(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED)
                .hasFieldOrPropertyWithValue("errorCode", "AUTHENTICATION_REQUIRED");
    }

    // Ticket Status Transition Tests

    @Test
    void updateTicketStatus_activeToCancelled_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.ACTIVE);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

        TicketDetailResponse response = ticketService.updateTicketStatus(1L, TicketStatus.CANCELLED);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TicketStatus.CANCELLED);

        clearSecurityContext();
    }

    @Test
    void updateTicketStatus_activeToRefunded_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.ACTIVE);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

        TicketDetailResponse response = ticketService.updateTicketStatus(1L, TicketStatus.REFUNDED);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TicketStatus.REFUNDED);

        clearSecurityContext();
    }

    @Test
    void updateTicketStatus_activeToUsed_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.ACTIVE);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

        TicketDetailResponse response = ticketService.updateTicketStatus(1L, TicketStatus.USED);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TicketStatus.USED);

        clearSecurityContext();
    }

    @Test
    void updateTicketStatus_refundedToCancelled_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.REFUNDED);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

        TicketDetailResponse response = ticketService.updateTicketStatus(1L, TicketStatus.CANCELLED);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TicketStatus.CANCELLED);

        clearSecurityContext();
    }

    @Test
    void updateTicketStatus_cancelledToActive_invalid() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.CANCELLED);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateTicketStatus(1L, TicketStatus.ACTIVE))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_ALREADY_CANCELLED");

        clearSecurityContext();
    }

    @Test
    void updateTicketStatus_usedToActive_invalid() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.USED);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateTicketStatus(1L, TicketStatus.ACTIVE))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_ALREADY_USED");

        clearSecurityContext();
    }

    @Test
    void updateTicketStatus_refundedToActive_invalid() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.REFUNDED);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateTicketStatus(1L, TicketStatus.ACTIVE))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_STATUS_TRANSITION");

        clearSecurityContext();
    }

    @Test
    void updateTicketStatus_refundedToUsed_invalid() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.REFUNDED);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateTicketStatus(1L, TicketStatus.USED))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_STATUS_TRANSITION");

        clearSecurityContext();
    }

    @Test
    void cancelTicket_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.ACTIVE);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

        TicketDetailResponse response = ticketService.cancelTicket(1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TicketStatus.CANCELLED);

        clearSecurityContext();
    }

    @Test
    void refundTicket_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.ACTIVE);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

        TicketDetailResponse response = ticketService.refundTicket(1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TicketStatus.REFUNDED);

        clearSecurityContext();
    }

    @Test
    void markTicketAsUsed_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.ACTIVE);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

        TicketDetailResponse response = ticketService.markTicketAsUsed(1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TicketStatus.USED);

        clearSecurityContext();
    }

    @Test
    void updateTicketStatus_unauthorized() {
        setupSecurityContext("otheruser", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.ACTIVE);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateTicketStatus(1L, TicketStatus.CANCELLED))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_ACCESS_DENIED");

        clearSecurityContext();
    }

    @Test
    void updateTicketStatus_legacyIssuedToCancelled_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.ISSUED); // Legacy status

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

        TicketDetailResponse response = ticketService.updateTicketStatus(1L, TicketStatus.CANCELLED);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TicketStatus.CANCELLED);

        clearSecurityContext();
    }

    @Test
    void updateTicketStatus_legacyVoidToActive_invalid() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Passenger passenger = buildPassenger(1L);
        Flight flight = buildFlight(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.VOID); // Legacy status

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateTicketStatus(1L, TicketStatus.ACTIVE))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_ALREADY_CANCELLED");

        clearSecurityContext();
    }
}
