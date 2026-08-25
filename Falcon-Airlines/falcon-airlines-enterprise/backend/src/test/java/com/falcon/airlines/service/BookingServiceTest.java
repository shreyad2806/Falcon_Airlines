package com.falcon.airlines.service;

import com.falcon.airlines.common.BaseUnitTest;
import com.falcon.airlines.dto.request.BookingRequest;
import com.falcon.airlines.dto.request.SeatAssignmentRequest;
import com.falcon.airlines.dto.response.BookingResponse;
import com.falcon.airlines.dto.response.BookingHistoryResponse;
import com.falcon.airlines.dto.response.SeatAvailabilityResponse;
import com.falcon.airlines.entity.Booking;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.entity.Seat;
import com.falcon.airlines.entity.SeatAllocation;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.entity.Aircraft;
import com.falcon.airlines.enums.BookingPaymentStatus;
import com.falcon.airlines.enums.BookingStatus;
import com.falcon.airlines.enums.FlightStatus;
import com.falcon.airlines.enums.TicketStatus;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.repository.BookingRepository;
import com.falcon.airlines.repository.FlightRepository;
import com.falcon.airlines.repository.PassengerRepository;
import com.falcon.airlines.repository.SeatAllocationRepository;
import com.falcon.airlines.repository.SeatRepository;
import com.falcon.airlines.repository.TicketRepository;
import com.falcon.airlines.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BookingServiceTest extends BaseUnitTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatAllocationRepository seatAllocationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private BookingRequest buildBookingRequest() {
        BookingRequest request = new BookingRequest();
        request.setCustomerId(1L);
        request.setFlightId(1L);
        request.setRequestedSeats(List.of("1A", "1B"));
        
        BookingRequest.BookingPassengerRequest passenger1 = new BookingRequest.BookingPassengerRequest();
        passenger1.setPassengerId(1L);
        passenger1.setFareClass("ECONOMY");
        
        BookingRequest.BookingPassengerRequest passenger2 = new BookingRequest.BookingPassengerRequest();
        passenger2.setPassengerId(2L);
        passenger2.setFareClass("ECONOMY");
        
        request.setPassengers(List.of(passenger1, passenger2));
        return request;
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("customer");
        return user;
    }

    private Flight buildFlight() {
        Flight flight = new Flight();
        flight.setId(1L);
        flight.setFlightNumber("FL001");
        flight.setStatus(FlightStatus.SCHEDULED);
        flight.setIsActive(true);
        flight.setScheduledDeparture(Instant.now().plusSeconds(3600));
        Aircraft aircraft = new Aircraft();
        aircraft.setId(1L);
        aircraft.setRegistrationNumber("TEST001");
        flight.setAircraft(aircraft);
        return flight;
    }

    private Passenger buildPassenger(Long id) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        passenger.setFirstName("John");
        passenger.setLastName("Doe");
        return passenger;
    }

    private Seat buildSeat(Long id, String seatNumber) {
        Seat seat = new Seat();
        seat.setId(id);
        seat.setSeatNumber(seatNumber);
        seat.setSeatClass("ECONOMY");
        seat.setIsActive(true);
        return seat;
    }

    private Booking buildBooking() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("BK123456");
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(BigDecimal.valueOf(200.00));
        booking.setCurrency("USD");
        booking.setBookingDate(Instant.now());
        booking.setPaymentStatus(BookingPaymentStatus.PENDING);
        booking.setVersion(0L);
        booking.setCustomer(buildUser());
        return booking;
    }

    private Ticket buildTicket(Long id) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTicketNumber("TKT123456");
        ticket.setStatus(TicketStatus.ISSUED);
        ticket.setFare(BigDecimal.valueOf(100.00));
        ticket.setTaxes(BigDecimal.valueOf(20.00));
        ticket.setIssuedAt(Instant.now());
        ticket.setBooking(buildBooking());
        ticket.setPassenger(buildPassenger(1L));
        ticket.setFlight(buildFlight());
        return ticket;
    }

    @Test
    void createBooking_success() {
        BookingRequest request = buildBookingRequest();
        User user = buildUser();
        Flight flight = buildFlight();
        Passenger passenger1 = buildPassenger(1L);
        Passenger passenger2 = buildPassenger(2L);
        Seat seat1 = buildSeat(1L, "1A");
        Seat seat2 = buildSeat(2L, "1B");
        Booking booking = buildBooking();
        Ticket ticket1 = buildTicket(1L);
        Ticket ticket2 = buildTicket(2L);

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        lenient().when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        lenient().when(passengerRepository.existsById(1L)).thenReturn(true);
        lenient().when(passengerRepository.existsById(2L)).thenReturn(true);
        lenient().when(seatRepository.findByAircraftIdAndSeatNumber(1L, "1A")).thenReturn(Optional.of(seat1));
        lenient().when(seatRepository.findByAircraftIdAndSeatNumber(1L, "1B")).thenReturn(Optional.of(seat2));
        lenient().when(seatAllocationRepository.existsBySeatIdAndFlightId(any(), eq(1L))).thenReturn(false);
        lenient().when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        lenient().when(passengerRepository.findById(1L)).thenReturn(Optional.of(passenger1));
        lenient().when(passengerRepository.findById(2L)).thenReturn(Optional.of(passenger2));
        lenient().when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket1, ticket2);
        lenient().when(ticketRepository.findByBookingId(1L)).thenReturn(List.of(ticket1, ticket2));
        lenient().when(seatAllocationRepository.findByTicketId(any())).thenReturn(Optional.empty());

        BookingResponse response = bookingService.createBooking(request);

        assertThat(response).isNotNull();
        assertThat(response.getBookingReference()).isEqualTo("BK123456");
        verify(bookingRepository).save(any(Booking.class));
        verify(ticketRepository, times(2)).save(any(Ticket.class));
        verify(seatAllocationRepository, times(2)).save(any(SeatAllocation.class));
    }

    @Test
    void createBooking_customerNotFound() {
        BookingRequest request = buildBookingRequest();

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "CUSTOMER_NOT_FOUND");
    }

    @Test
    void createBooking_flightNotFound() {
        BookingRequest request = buildBookingRequest();
        User user = buildUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(flightRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "FLIGHT_NOT_FOUND");
    }

    @Test
    void createBooking_flightCancelled() {
        BookingRequest request = buildBookingRequest();
        User user = buildUser();
        Flight flight = buildFlight();
        flight.setStatus(FlightStatus.CANCELLED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "FLIGHT_CANCELLED");
    }

    @Test
    void createBooking_duplicateSeatsInRequest() {
        BookingRequest request = buildBookingRequest();
        request.setRequestedSeats(List.of("1A", "1A"));
        request.setPassengers(List.of(new BookingRequest.BookingPassengerRequest()));
        User user = buildUser();
        Flight flight = buildFlight();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "DUPLICATE_SEATS_IN_REQUEST");
    }

    @Test
    void createBooking_seatPassengerMismatch() {
        BookingRequest request = buildBookingRequest();
        request.setRequestedSeats(List.of("1A"));
        User user = buildUser();
        Flight flight = buildFlight();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "SEAT_PASSENGER_MISMATCH");
    }

    @Test
    void createBooking_passengerNotFound() {
        BookingRequest request = buildBookingRequest();
        User user = buildUser();
        Flight flight = buildFlight();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(passengerRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "PASSENGER_NOT_FOUND");
    }

    @Test
    void createBooking_seatNotFound() {
        BookingRequest request = buildBookingRequest();
        User user = buildUser();
        Flight flight = buildFlight();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(passengerRepository.existsById(1L)).thenReturn(true);
        when(passengerRepository.existsById(2L)).thenReturn(true);
        when(seatRepository.findByAircraftIdAndSeatNumber(1L, "1A")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "SEAT_NOT_FOUND");
    }

    @Test
    void createBooking_seatInactive() {
        BookingRequest request = buildBookingRequest();
        User user = buildUser();
        Flight flight = buildFlight();
        Seat seat = buildSeat(1L, "1A");
        seat.setIsActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(passengerRepository.existsById(1L)).thenReturn(true);
        when(passengerRepository.existsById(2L)).thenReturn(true);
        when(seatRepository.findByAircraftIdAndSeatNumber(1L, "1A")).thenReturn(Optional.of(seat));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "SEAT_INACTIVE");
    }

    @Test
    void createBooking_seatAlreadyAllocated() {
        BookingRequest request = buildBookingRequest();
        User user = buildUser();
        Flight flight = buildFlight();
        Seat seat1 = buildSeat(1L, "1A");
        Seat seat2 = buildSeat(2L, "1B");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(passengerRepository.existsById(1L)).thenReturn(true);
        when(passengerRepository.existsById(2L)).thenReturn(true);
        when(seatRepository.findByAircraftIdAndSeatNumber(1L, "1A")).thenReturn(Optional.of(seat1));
        when(seatRepository.findByAircraftIdAndSeatNumber(1L, "1B")).thenReturn(Optional.of(seat2));
        when(seatAllocationRepository.existsBySeatIdAndFlightId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "SEAT_ALREADY_ALLOCATED");
    }

    @Test
    void getBooking_success() {
        Booking booking = buildBooking();
        Ticket ticket = buildTicket(1L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(ticketRepository.findByBookingId(1L)).thenReturn(List.of(ticket));

        BookingResponse response = bookingService.getBooking(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        verify(bookingRepository).findById(1L);
    }

    @Test
    void getBooking_notFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBooking(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "BOOKING_NOT_FOUND");
    }

    @Test
    void cancelBooking_success() {
        Booking booking = buildBooking();
        Ticket ticket = buildTicket(1L);
        SeatAllocation allocation = new SeatAllocation();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(ticketRepository.findByBookingId(1L)).thenReturn(List.of(ticket));
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.of(allocation));

        bookingService.cancelBooking(1L, "Customer request");

        verify(bookingRepository).save(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(seatAllocationRepository).delete(allocation);
    }

    @Test
    void cancelBooking_alreadyCancelled() {
        Booking booking = buildBooking();
        booking.setStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L, "Reason"))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "BOOKING_ALREADY_CANCELLED");
    }

    @Test
    void cancelBooking_alreadyCompleted() {
        Booking booking = buildBooking();
        booking.setStatus(BookingStatus.COMPLETED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L, "Reason"))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("errorCode", "BOOKING_ALREADY_COMPLETED");
    }

    @Test
    void getBookingHistory_success() {
        User user = buildUser();
        Booking booking = buildBooking();
        Ticket ticket = buildTicket(1L);
        Page<Booking> page = new PageImpl<>(List.of(booking));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findByCustomerId(1L, PageRequest.of(0, 10))).thenReturn(page);
        when(ticketRepository.findByBookingId(1L)).thenReturn(List.of(ticket));

        BookingHistoryResponse response = bookingService.getBookingHistory(1L, 0, 10);

        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo(1L);
        assertThat(response.getTotalBookings()).isEqualTo(1);
        assertThat(response.getBookings()).hasSize(1);
    }

    @Test
    void checkSeatAvailability_success() {
        Flight flight = buildFlight();
        Seat seat1 = buildSeat(1L, "1A");
        Seat seat2 = buildSeat(2L, "1B");

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(seatRepository.findByAircraftIdAndIsActiveTrue(1L)).thenReturn(List.of(seat1, seat2));
        when(seatRepository.findAvailableSeatsForFlight(1L, 1L)).thenReturn(List.of(seat1));

        SeatAvailabilityResponse response = bookingService.checkSeatAvailability(1L);

        assertThat(response).isNotNull();
        assertThat(response.getFlightId()).isEqualTo(1L);
        assertThat(response.getTotalSeats()).isEqualTo(2);
        assertThat(response.getAvailableSeats()).isEqualTo(1);
        assertThat(response.getSeats()).hasSize(2);
    }

    @Test
    void assignSeat_success() {
        SeatAssignmentRequest request = new SeatAssignmentRequest();
        request.setTicketId(1L);
        request.setFlightId(1L);
        request.setSeatNumber("1A");

        Ticket ticket = buildTicket(1L);
        Flight flight = buildFlight();
        Seat seat = buildSeat(1L, "1A");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());
        when(seatRepository.findByAircraftIdAndSeatNumber(1L, "1A")).thenReturn(Optional.of(seat));
        when(seatAllocationRepository.existsBySeatIdAndFlightId(1L, 1L)).thenReturn(false);

        bookingService.assignSeat(request);

        verify(seatAllocationRepository).save(any(SeatAllocation.class));
    }

    @Test
    void assignSeat_ticketNotFound() {
        SeatAssignmentRequest request = new SeatAssignmentRequest();
        request.setTicketId(1L);
        request.setFlightId(1L);
        request.setSeatNumber("1A");

        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.assignSeat(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_NOT_FOUND");
    }

    @Test
    void assignSeat_seatAlreadyAllocated() {
        SeatAssignmentRequest request = new SeatAssignmentRequest();
        request.setTicketId(1L);
        request.setFlightId(1L);
        request.setSeatNumber("1A");

        Ticket ticket = buildTicket(1L);
        Flight flight = buildFlight();
        Seat seat = buildSeat(1L, "1A");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());
        when(seatRepository.findByAircraftIdAndSeatNumber(1L, "1A")).thenReturn(Optional.of(seat));
        when(seatAllocationRepository.existsBySeatIdAndFlightId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> bookingService.assignSeat(request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("errorCode", "SEAT_ALREADY_ALLOCATED");
    }

    @Test
    void releaseSeat_success() {
        Ticket ticket = buildTicket(1L);
        SeatAllocation allocation = new SeatAllocation();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.of(allocation));

        bookingService.releaseSeat(1L);

        verify(seatAllocationRepository).delete(allocation);
    }

    @Test
    void releaseSeat_ticketNotFound() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.releaseSeat(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "TICKET_NOT_FOUND");
    }

    @Test
    void releaseSeat_noAllocationFound() {
        Ticket ticket = buildTicket(1L);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.releaseSeat(1L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("errorCode", "SEAT_ALLOCATION_NOT_FOUND");
    }
}
