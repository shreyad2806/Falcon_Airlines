package com.falcon.airlines.repository;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.entity.Booking;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.entity.Airport;
import com.falcon.airlines.enums.BookingStatus;
import com.falcon.airlines.enums.BookingPaymentStatus;
import com.falcon.airlines.enums.TicketStatus;
import com.falcon.airlines.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TicketRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Test
    void saveAndFindTicket() {
        Booking booking = createBooking();
        Passenger passenger = createPassenger();
        Flight flight = createFlight();
        Ticket ticket = createTicket(booking, passenger, flight);
        
        Ticket saved = ticketRepository.save(ticket);
        assertThat(saved.getId()).isNotNull();

        Optional<Ticket> found = ticketRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTicketNumber()).isEqualTo("TKT001");
    }

    @Test
    void findByTicketNumber() {
        Booking booking = createBooking();
        Passenger passenger = createPassenger();
        Flight flight = createFlight();
        Ticket ticket = createTicket(booking, passenger, flight);
        ticketRepository.save(ticket);

        Optional<Ticket> found = ticketRepository.findByTicketNumber("TKT001");
        assertThat(found).isPresent();
        assertThat(found.get().getPassenger().getId()).isEqualTo(passenger.getId());
    }

    @Test
    void findByTicketNumber_notFound() {
        Optional<Ticket> found = ticketRepository.findByTicketNumber("NOTFOUND");
        assertThat(found).isEmpty();
    }

    @Test
    void findByBookingId() {
        Booking booking = createBooking();
        Passenger passenger = createPassenger();
        Flight flight = createFlight();
        
        Ticket ticket1 = createTicket(booking, passenger, flight);
        ticket1.setTicketNumber("TKT001");
        Ticket ticket2 = createTicket(booking, passenger, flight);
        ticket2.setTicketNumber("TKT002");
        ticketRepository.save(ticket1);
        ticketRepository.save(ticket2);

        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        assertThat(tickets).hasSize(2);
    }

    @Test
    void findByPassengerId() {
        Booking booking = createBooking();
        Passenger passenger = createPassenger();
        Flight flight = createFlight();
        
        Ticket ticket1 = createTicket(booking, passenger, flight);
        ticket1.setTicketNumber("TKT003");
        Ticket ticket2 = createTicket(booking, passenger, flight);
        ticket2.setTicketNumber("TKT004");
        ticketRepository.save(ticket1);
        ticketRepository.save(ticket2);

        List<Ticket> tickets = ticketRepository.findByPassengerId(passenger.getId());
        assertThat(tickets).hasSize(2);
    }

    @Test
    void findByFlightId() {
        Booking booking = createBooking();
        Passenger passenger = createPassenger();
        Flight flight = createFlight();
        
        Ticket ticket1 = createTicket(booking, passenger, flight);
        ticket1.setTicketNumber("TKT005");
        Ticket ticket2 = createTicket(booking, passenger, flight);
        ticket2.setTicketNumber("TKT006");
        ticketRepository.save(ticket1);
        ticketRepository.save(ticket2);

        List<Ticket> tickets = ticketRepository.findByFlightId(flight.getId());
        assertThat(tickets).hasSize(2);
    }

    @Test
    void existsByPassengerId() {
        Booking booking = createBooking();
        Passenger passenger = createPassenger();
        Flight flight = createFlight();
        Ticket ticket = createTicket(booking, passenger, flight);
        ticketRepository.save(ticket);

        boolean exists = ticketRepository.existsByPassengerId(passenger.getId());
        assertThat(exists).isTrue();

        boolean notExists = ticketRepository.existsByPassengerId(999L);
        assertThat(notExists).isFalse();
    }

    @Test
    void findSeatAllocationByTicketId() {
        Booking booking = createBooking();
        Passenger passenger = createPassenger();
        Flight flight = createFlight();
        Ticket ticket = createTicket(booking, passenger, flight);
        ticketRepository.save(ticket);

        // Create a seat allocation for this ticket
        com.falcon.airlines.entity.SeatAllocation allocation = new com.falcon.airlines.entity.SeatAllocation();
        allocation.setTicket(ticket);
        allocation.setFlight(flight);
        seatAllocationRepository.save(allocation);

        Optional<com.falcon.airlines.entity.SeatAllocation> found = ticketRepository.findSeatAllocationByTicketId(ticket.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTicket().getId()).isEqualTo(ticket.getId());
    }

    @Test
    void findSeatAllocationByTicketId_notFound() {
        Optional<com.falcon.airlines.entity.SeatAllocation> found = ticketRepository.findSeatAllocationByTicketId(999L);
        assertThat(found).isEmpty();
    }

    @Test
    void versionFieldPresent() {
        Booking booking = createBooking();
        Passenger passenger = createPassenger();
        Flight flight = createFlight();
        Ticket ticket = createTicket(booking, passenger, flight);
        Ticket saved = ticketRepository.save(ticket);
        
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0L);
    }

    @Test
    void versionIncrementedOnUpdate() {
        Booking booking = createBooking();
        Passenger passenger = createPassenger();
        Flight flight = createFlight();
        Ticket ticket = createTicket(booking, passenger, flight);
        Ticket saved = ticketRepository.save(ticket);
        Long initialVersion = saved.getVersion();
        
        saved.setStatus(TicketStatus.VOID);
        Ticket updated = ticketRepository.save(saved);
        
        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
    }

    private Booking createBooking() {
        User customer = new User();
        customer.setUsername("cust_test");
        customer.setEmail("cust_test@example.com");
        customer.setPasswordHash("hashed_password");
        customer.setStatus(UserStatus.ACTIVE);
        User savedCustomer = userRepository.save(customer);

        Booking booking = new Booking();
        booking.setBookingReference("REF001");
        booking.setCustomer(savedCustomer);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(BigDecimal.valueOf(100.00));
        booking.setCurrency("USD");
        booking.setBookingDate(Instant.now());
        booking.setPaymentStatus(BookingPaymentStatus.PENDING);
        return bookingRepository.save(booking);
    }

    private Passenger createPassenger() {
        Passenger passenger = new Passenger();
        passenger.setFirstName("John");
        passenger.setLastName("Doe");
        passenger.setDateOfBirth(LocalDate.of(1990, 1, 1));
        passenger.setEmail("john.doe@example.com");
        passenger.setPhone("+1234567890");
        passenger.setPassportNumber("AB1234567");
        passenger.setNationality("USA");
        passenger.setGender(com.falcon.airlines.enums.Gender.M);
        return passengerRepository.save(passenger);
    }

    private Flight createFlight() {
        Airport origin = createAirport("JFK", "John F. Kennedy International");
        Airport destination = createAirport("LAX", "Los Angeles International");
        
        Flight flight = new Flight();
        flight.setFlightNumber("FL001");
        flight.setOriginAirport(origin);
        flight.setDestinationAirport(destination);
        flight.setScheduledDeparture(Instant.now().plusSeconds(3600));
        flight.setScheduledArrival(Instant.now().plusSeconds(7200));
        return flightRepository.save(flight);
    }

    private Airport createAirport(String iataCode, String name) {
        Airport airport = new Airport();
        airport.setIataCode(iataCode);
        airport.setName(name);
        airport.setCity("Test City");
        airport.setCountry("US");
        airport.setTimeZone("UTC");
        airport.setIsActive(true);
        return airportRepository.save(airport);
    }

    private Ticket createTicket(Booking booking, Passenger passenger, Flight flight) {
        Ticket ticket = new Ticket();
        ticket.setTicketNumber("TKT001");
        ticket.setBooking(booking);
        ticket.setPassenger(passenger);
        ticket.setFlight(flight);
        ticket.setFareBasis("ECONOMY");
        ticket.setFare(BigDecimal.valueOf(100.00));
        ticket.setTaxes(BigDecimal.valueOf(20.00));
        ticket.setStatus(TicketStatus.ISSUED);
        ticket.setIssuedAt(Instant.now());
        return ticket;
    }
}
