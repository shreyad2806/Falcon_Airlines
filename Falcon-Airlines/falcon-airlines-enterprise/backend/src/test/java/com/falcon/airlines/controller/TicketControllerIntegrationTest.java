package com.falcon.airlines.controller;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.entity.Aircraft;
import com.falcon.airlines.entity.Airport;
import com.falcon.airlines.entity.Booking;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.entity.Seat;
import com.falcon.airlines.entity.SeatAllocation;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.enums.BookingPaymentStatus;
import com.falcon.airlines.enums.BookingStatus;
import com.falcon.airlines.enums.TicketStatus;
import com.falcon.airlines.repository.AircraftRepository;
import com.falcon.airlines.repository.AirportRepository;
import com.falcon.airlines.repository.BookingRepository;
import com.falcon.airlines.repository.FlightRepository;
import com.falcon.airlines.repository.PassengerRepository;
import com.falcon.airlines.repository.SeatAllocationRepository;
import com.falcon.airlines.repository.SeatRepository;
import com.falcon.airlines.repository.TicketRepository;
import com.falcon.airlines.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class TicketControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    private User customer;
    private Booking booking;
    private Ticket ticket;
    private int testCounter = 0;

    @BeforeEach
    void setUp() {
        testCounter++;
        customer = createCustomer("tkt_customer_" + testCounter);
        booking = createBooking(customer);
        ticket = createTicket(booking);
    }

    @AfterEach
    void tearDown() {
        seatAllocationRepository.deleteAll();
        seatRepository.deleteAll();
        ticketRepository.deleteAll();
        bookingRepository.deleteAll();
        passengerRepository.deleteAll();
        flightRepository.deleteAll();
        aircraftRepository.deleteAll();
        airportRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "TICKET_READ")
    void getTicketById_success() throws Exception {
        mockMvc.perform(get("/api/tickets/" + ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(ticket.getId()))
                .andExpect(jsonPath("$.data.ticketNumber").value(ticket.getTicketNumber()));
    }

    @Test
    @WithMockUser(authorities = "TICKET_READ")
    void getTicketById_notFound() throws Exception {
        mockMvc.perform(get("/api/tickets/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTicketById_unauthorized() throws Exception {
        mockMvc.perform(get("/api/tickets/" + ticket.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "TICKET_READ")
    void getTicketByNumber_success() throws Exception {
        mockMvc.perform(get("/api/tickets/number/" + ticket.getTicketNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ticketNumber").value(ticket.getTicketNumber()));
    }

    @Test
    @WithMockUser(authorities = "TICKET_READ")
    void getTicketsByBookingId_success() throws Exception {
        mockMvc.perform(get("/api/tickets/booking/" + booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "TICKET_READ")
    void getTicketsByPassengerId_success() throws Exception {
        mockMvc.perform(get("/api/tickets/passenger/" + ticket.getPassenger().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "TICKET_WRITE")
    void regenerateTicketForBooking_notImplemented() throws Exception {
        mockMvc.perform(post("/api/tickets/booking/" + booking.getId() + "/regenerate"))
                .andExpect(status().isNotImplemented());
    }

    // Helper methods

    private User createCustomer(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hashed_password");
        user.setStatus(com.falcon.airlines.enums.UserStatus.ACTIVE);
        user.setMfaEnabled(false);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private Booking createBooking(User customer) {
        Airport origin = airportRepository.findByIataCode("JFK")
                .orElseGet(() -> {
                    Airport airport = new Airport();
                    airport.setIataCode("JFK");
                    airport.setIcaoCode("KJFK");
                    airport.setName("John F. Kennedy International");
                    airport.setCity("New York");
                    airport.setCountry("US");
                    airport.setTimeZone("America/New_York");
                    airport.setIsActive(true);
                    return airportRepository.save(airport);
                });

        Airport destination = airportRepository.findByIataCode("LAX")
                .orElseGet(() -> {
                    Airport airport = new Airport();
                    airport.setIataCode("LAX");
                    airport.setIcaoCode("KLAX");
                    airport.setName("Los Angeles International");
                    airport.setCity("Los Angeles");
                    airport.setCountry("US");
                    airport.setTimeZone("America/Los_Angeles");
                    airport.setIsActive(true);
                    return airportRepository.save(airport);
                });

        Aircraft aircraft = new Aircraft();
        aircraft.setRegistrationNumber("TKT" + testCounter);
        aircraft.setType("COMMERCIAL");
        aircraft.setModel("Boeing 737");
        aircraft.setManufacturer("Boeing");
        aircraft.setTotalCapacity((short) 150);
        aircraft = aircraftRepository.save(aircraft);

        Flight flight = new Flight();
        flight.setFlightNumber("TKT" + testCounter);
        flight.setOriginAirport(origin);
        flight.setDestinationAirport(destination);
        flight.setAircraft(aircraft);
        flight.setScheduledDeparture(Instant.now().plusSeconds(86400));
        flight.setScheduledArrival(Instant.now().plusSeconds(172800));
        flight.setStatus(com.falcon.airlines.enums.FlightStatus.SCHEDULED);
        flight.setIsActive(true);
        flight.setTerminal("T1");
        flight.setGate("A1");
        flight = flightRepository.save(flight);

        Passenger passenger = new Passenger();
        passenger.setFirstName("John");
        passenger.setLastName("Doe");
        passenger.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        passenger.setEmail("john.doe@example.com");
        passenger.setPhone("+1234567890");
        passenger.setPassportNumber("PP123456");
        passenger.setNationality("USA");
        passenger.setGender(com.falcon.airlines.enums.Gender.M);
        passenger = passengerRepository.save(passenger);

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingReference("TKT" + testCounter);
        booking.setTotalAmount(BigDecimal.valueOf(200.00));
        booking.setCurrency("USD");
        booking.setBookingDate(Instant.now());
        booking.setPaymentStatus(BookingPaymentStatus.PAID);
        return bookingRepository.save(booking);
    }

    private Ticket createTicket(Booking booking) {
        Flight flight = flightRepository.findAll().get(0);
        Passenger passenger = passengerRepository.findAll().get(0);
        Aircraft aircraft = aircraftRepository.findAll().get(0);

        Ticket ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setPassenger(passenger);
        ticket.setFlight(flight);
        ticket.setTicketNumber("TKT" + System.currentTimeMillis());
        ticket.setFareBasis("ECONOMY");
        ticket.setFare(BigDecimal.valueOf(100.00));
        ticket.setTaxes(BigDecimal.valueOf(20.00));
        ticket.setStatus(TicketStatus.ISSUED);
        ticket.setIssuedAt(Instant.now());
        ticket = ticketRepository.save(ticket);

        Seat seat = new Seat();
        seat.setAircraft(aircraft);
        seat.setSeatNumber("1A");
        seat.setSeatClass("ECONOMY");
        seat.setRowNumber((short) 1);
        seat.setColumnLetter("A");
        seat.setIsActive(true);
        seat = seatRepository.save(seat);

        SeatAllocation allocation = new SeatAllocation();
        allocation.setSeat(seat);
        allocation.setTicket(ticket);
        allocation.setFlight(flight);
        allocation.setAllocatedAt(Instant.now());
        seatAllocationRepository.save(allocation);

        return ticket;
    }
}
