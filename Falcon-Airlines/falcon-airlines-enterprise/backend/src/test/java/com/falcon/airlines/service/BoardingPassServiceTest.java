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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoardingPassServiceTest {

    @Mock
    private BoardingPassRepository boardingPassRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatAllocationRepository seatAllocationRepository;

    private BoardingPassService boardingPassService;

    @BeforeEach
    void setUp() {
        boardingPassRepository = mock(BoardingPassRepository.class);
        ticketRepository = mock(TicketRepository.class);
        bookingRepository = mock(BookingRepository.class);
        seatAllocationRepository = mock(SeatAllocationRepository.class);

        boardingPassService = new BoardingPassService(
                boardingPassRepository,
                ticketRepository,
                bookingRepository,
                seatAllocationRepository
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Boarding Pass Generation Tests

    @Test
    void generateBoardingPass_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Flight flight = buildFlight(1L);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.ACTIVE);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(boardingPassRepository.findByTicketId(1L)).thenReturn(List.of());
        when(boardingPassRepository.save(any(BoardingPass.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

        BoardingPassResponse response = boardingPassService.generateBoardingPass(1L);

        assertThat(response).isNotNull();
        assertThat(response.getTicketId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(BoardingPassStatus.GENERATED);

        clearSecurityContext();
    }

    @Test
    void generateBoardingPass_ticketNotFound() {
        setupSecurityContext("customer", "CUSTOMER");
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardingPassService.generateBoardingPass(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_NOT_FOUND");

        clearSecurityContext();
    }

    @Test
    void generateBoardingPass_ticketNotActive() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Flight flight = buildFlight(1L);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.CANCELLED);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> boardingPassService.generateBoardingPass(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_NOT_ACTIVE");

        clearSecurityContext();
    }

    @Test
    void generateBoardingPass_bookingNotConfirmed() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        booking.setStatus(BookingStatus.CANCELLED);
        Flight flight = buildFlight(1L);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.ACTIVE);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> boardingPassService.generateBoardingPass(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "BOOKING_NOT_CONFIRMED");

        clearSecurityContext();
    }

    @Test
    void generateBoardingPass_flightNotScheduled() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Flight flight = buildFlight(1L);
        flight.setStatus(FlightStatus.CANCELLED);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.ACTIVE);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> boardingPassService.generateBoardingPass(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "FLIGHT_NOT_SCHEDULED");

        clearSecurityContext();
    }

    @Test
    void generateBoardingPass_alreadyExists() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Flight flight = buildFlight(1L);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.ACTIVE);

        BoardingPass existingPass = new BoardingPass();
        existingPass.setId(1L);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(boardingPassRepository.findByTicketId(1L)).thenReturn(List.of(existingPass));

        assertThatThrownBy(() -> boardingPassService.generateBoardingPass(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "BOARDING_PASS_ALREADY_EXISTS");

        clearSecurityContext();
    }

    @Test
    void generateBoardingPass_unauthorized() {
        setupSecurityContext("otheruser", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Flight flight = buildFlight(1L);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        ticket.setStatus(TicketStatus.ACTIVE);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> boardingPassService.generateBoardingPass(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_ACCESS_DENIED");

        clearSecurityContext();
    }

    // Boarding Pass Retrieval Tests

    @Test
    void getBoardingPassById_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Flight flight = buildFlight(1L);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        BoardingPass boardingPass = buildBoardingPass(1L, ticket, passenger, flight, booking);

        when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(boardingPass));
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

        BoardingPassResponse response = boardingPassService.getBoardingPassById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);

        clearSecurityContext();
    }

    @Test
    void getBoardingPassById_notFound() {
        setupSecurityContext("customer", "CUSTOMER");
        when(boardingPassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardingPassService.getBoardingPassById(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "BOARDING_PASS_NOT_FOUND");

        clearSecurityContext();
    }

    // Boarding Pass Status Update Tests

    @Test
    void updateBoardingPassStatus_generatedToCheckedIn_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Flight flight = buildFlight(1L);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        BoardingPass boardingPass = buildBoardingPass(1L, ticket, passenger, flight, booking);
        boardingPass.setStatus(BoardingPassStatus.GENERATED);

        when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(boardingPass));
        when(boardingPassRepository.save(any(BoardingPass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoardingPassResponse response = boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.CHECKED_IN);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BoardingPassStatus.CHECKED_IN);
        assertThat(response.getCheckedInAt()).isNotNull();

        clearSecurityContext();
    }

    @Test
    void updateBoardingPassStatus_checkedInToBoarding_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Flight flight = buildFlight(1L);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        BoardingPass boardingPass = buildBoardingPass(1L, ticket, passenger, flight, booking);
        boardingPass.setStatus(BoardingPassStatus.CHECKED_IN);

        when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(boardingPass));
        when(boardingPassRepository.save(any(BoardingPass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoardingPassResponse response = boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.BOARDING);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BoardingPassStatus.BOARDING);

        clearSecurityContext();
    }

    @Test
    void updateBoardingPassStatus_boardingToUsed_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Flight flight = buildFlight(1L);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        BoardingPass boardingPass = buildBoardingPass(1L, ticket, passenger, flight, booking);
        boardingPass.setStatus(BoardingPassStatus.BOARDING);

        when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(boardingPass));
        when(boardingPassRepository.save(any(BoardingPass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoardingPassResponse response = boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.USED);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BoardingPassStatus.USED);
        assertThat(response.getBoardedAt()).isNotNull();

        clearSecurityContext();
    }

    @Test
    void updateBoardingPassStatus_voidToUsed_invalid() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Flight flight = buildFlight(1L);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        BoardingPass boardingPass = buildBoardingPass(1L, ticket, passenger, flight, booking);
        boardingPass.setStatus(BoardingPassStatus.VOID);

        when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(boardingPass));

        assertThatThrownBy(() -> boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.USED))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "BOARDING_PASS_ALREADY_VOID");

        clearSecurityContext();
    }

    @Test
    void updateBoardingPassStatus_usedToCheckedIn_invalid() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Flight flight = buildFlight(1L);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        BoardingPass boardingPass = buildBoardingPass(1L, ticket, passenger, flight, booking);
        boardingPass.setStatus(BoardingPassStatus.USED);

        when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(boardingPass));

        assertThatThrownBy(() -> boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.CHECKED_IN))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "BOARDING_PASS_ALREADY_USED");

        clearSecurityContext();
    }

    @Test
    void checkInBoardingPass_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Flight flight = buildFlight(1L);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        BoardingPass boardingPass = buildBoardingPass(1L, ticket, passenger, flight, booking);
        boardingPass.setStatus(BoardingPassStatus.GENERATED);

        when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(boardingPass));
        when(boardingPassRepository.save(any(BoardingPass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoardingPassResponse response = boardingPassService.checkInBoardingPass(1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BoardingPassStatus.CHECKED_IN);

        clearSecurityContext();
    }

    @Test
    void boardPassenger_success() {
        setupSecurityContext("customer", "CUSTOMER");
        User user = buildUser("customer");
        Booking booking = buildBooking(1L, user);
        Flight flight = buildFlight(1L);
        Passenger passenger = buildPassenger(1L);
        Ticket ticket = buildTicket(1L, booking, passenger, flight);
        BoardingPass boardingPass = buildBoardingPass(1L, ticket, passenger, flight, booking);
        boardingPass.setStatus(BoardingPassStatus.BOARDING);

        when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(boardingPass));
        when(boardingPassRepository.save(any(BoardingPass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoardingPassResponse response = boardingPassService.boardPassenger(1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BoardingPassStatus.USED);

        clearSecurityContext();
    }

    // Helper methods

    private void setupSecurityContext(String username, String role) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(username);
        lenient().when(authentication.getAuthorities()).thenReturn((List) List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role)));
        lenient().when(authentication.isAuthenticated()).thenReturn(true);

        org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hashed_password");
        return user;
    }

    private Booking buildBooking(Long id, User customer) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setCustomer(customer);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingReference("BK123456");
        booking.setBookingDate(Instant.now());
        return booking;
    }

    private Flight buildFlight(Long id) {
        Flight flight = new Flight();
        flight.setId(id);
        flight.setFlightNumber("FL001");
        flight.setStatus(FlightStatus.SCHEDULED);
        flight.setScheduledDeparture(Instant.now().plusSeconds(86400));
        flight.setScheduledArrival(Instant.now().plusSeconds(172800));
        flight.setTerminal("T1");
        flight.setGate("A1");
        return flight;
    }

    private Passenger buildPassenger(Long id) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        passenger.setFirstName("John");
        passenger.setLastName("Doe");
        passenger.setEmail("john.doe@example.com");
        return passenger;
    }

    private Ticket buildTicket(Long id, Booking booking, Passenger passenger, Flight flight) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setBooking(booking);
        ticket.setPassenger(passenger);
        ticket.setFlight(flight);
        ticket.setTicketNumber("TKT123456");
        ticket.setFareBasis("ECONOMY");
        ticket.setFare(BigDecimal.valueOf(100.00));
        ticket.setTaxes(BigDecimal.valueOf(20.00));
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setIssuedAt(Instant.now());
        ticket.setVersion(0L);
        return ticket;
    }

    private BoardingPass buildBoardingPass(Long id, Ticket ticket, Passenger passenger, Flight flight, Booking booking) {
        BoardingPass boardingPass = new BoardingPass();
        boardingPass.setId(id);
        boardingPass.setBoardingPassNumber("BP123456");
        boardingPass.setTicket(ticket);
        boardingPass.setPassenger(passenger);
        boardingPass.setFlight(flight);
        boardingPass.setBooking(booking);
        boardingPass.setStatus(BoardingPassStatus.GENERATED);
        boardingPass.setGeneratedAt(Instant.now());
        boardingPass.setVersion(0L);
        return boardingPass;
    }
}
