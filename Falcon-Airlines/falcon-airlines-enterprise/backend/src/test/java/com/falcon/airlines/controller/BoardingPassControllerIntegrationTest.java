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
import com.falcon.airlines.entity.BoardingPass;
import com.falcon.airlines.enums.BookingPaymentStatus;
import com.falcon.airlines.enums.BookingStatus;
import com.falcon.airlines.enums.FlightStatus;
import com.falcon.airlines.enums.TicketStatus;
import com.falcon.airlines.enums.BoardingPassStatus;
import com.falcon.airlines.repository.AircraftRepository;
import com.falcon.airlines.repository.AirportRepository;
import com.falcon.airlines.repository.BookingRepository;
import com.falcon.airlines.repository.FlightRepository;
import com.falcon.airlines.repository.PassengerRepository;
import com.falcon.airlines.repository.SeatAllocationRepository;
import com.falcon.airlines.repository.SeatRepository;
import com.falcon.airlines.repository.TicketRepository;
import com.falcon.airlines.repository.UserRepository;
import com.falcon.airlines.repository.BoardingPassRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@Transactional
class BoardingPassControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private BoardingPassRepository boardingPassRepository;

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

    @Autowired
    private com.falcon.airlines.util.QrTokenUtil qrTokenUtil;

    private User customer;
    private Booking booking;
    private Ticket ticket;
    private BoardingPass boardingPass;
    private Flight createdFlight;
    private Passenger createdPassenger;
    private Aircraft createdAircraft;
    private int testCounter = 0;

    @BeforeEach
    void setUp() {
        testCounter++;
        customer = createCustomer("bp_customer_" + testCounter);
        booking = createBooking(customer);
        ticket = createTicket(booking, createdFlight, createdPassenger, createdAircraft);
    }

    @AfterEach
    void tearDown() {
        boardingPassRepository.deleteAll();
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
    @WithMockUser(authorities = {"BOARDING_PASS_WRITE", "ROLE_ADMIN"})
    void generateBoardingPass_success() throws Exception {
        mockMvc.perform(post("/api/boarding-passes/ticket/" + ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ticketId").value(ticket.getId()))
                .andExpect(jsonPath("$.data.status").value(BoardingPassStatus.GENERATED.toString()));
    }

    @Test
    @WithMockUser(authorities = {"BOARDING_PASS_WRITE", "ROLE_ADMIN"})
    void generateBoardingPass_alreadyExists() throws Exception {
        // Create first boarding pass
        BoardingPass firstPass = createBoardingPass(ticket);
        boardingPassRepository.save(firstPass);

        mockMvc.perform(post("/api/boarding-passes/ticket/" + ticket.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = {"BOARDING_PASS_READ", "ROLE_ADMIN"})
    void getBoardingPassById_success() throws Exception {
        BoardingPass boardingPass = createBoardingPass(ticket);
        boardingPass = boardingPassRepository.save(boardingPass);

        mockMvc.perform(get("/api/boarding-passes/" + boardingPass.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(boardingPass.getId()))
                .andExpect(jsonPath("$.data.boardingPassNumber").value(boardingPass.getBoardingPassNumber()));
    }

    @Test
    @WithMockUser(authorities = {"BOARDING_PASS_READ", "ROLE_ADMIN"})
    void getBoardingPassById_notFound() throws Exception {
        mockMvc.perform(get("/api/boarding-passes/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"BOARDING_PASS_READ", "ROLE_ADMIN"})
    void getBoardingPassByNumber_success() throws Exception {
        BoardingPass boardingPass = createBoardingPass(ticket);
        boardingPass = boardingPassRepository.save(boardingPass);

        mockMvc.perform(get("/api/boarding-passes/number/" + boardingPass.getBoardingPassNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boardingPassNumber").value(boardingPass.getBoardingPassNumber()));
    }

    @Test
    @WithMockUser(authorities = {"BOARDING_PASS_READ", "ROLE_ADMIN"})
    void getBoardingPassesByBookingId_success() throws Exception {
        BoardingPass boardingPass = createBoardingPass(ticket);
        boardingPass = boardingPassRepository.save(boardingPass);

        mockMvc.perform(get("/api/boarding-passes/booking/" + booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = {"BOARDING_PASS_READ", "ROLE_ADMIN"})
    void getBoardingPassesByPassengerId_success() throws Exception {
        BoardingPass boardingPass = createBoardingPass(ticket);
        boardingPass = boardingPassRepository.save(boardingPass);

        mockMvc.perform(get("/api/boarding-passes/passenger/" + ticket.getPassenger().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = {"BOARDING_PASS_WRITE", "ROLE_ADMIN"})
    void updateBoardingPassStatus_success() throws Exception {
        BoardingPass boardingPass = createBoardingPass(ticket);
        boardingPass.setStatus(BoardingPassStatus.GENERATED);
        boardingPass = boardingPassRepository.save(boardingPass);

        mockMvc.perform(put("/api/boarding-passes/" + boardingPass.getId() + "/status")
                        .param("status", "CHECKED_IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value(BoardingPassStatus.CHECKED_IN.toString()));
    }

    @Test
    @WithMockUser(authorities = {"BOARDING_PASS_WRITE", "ROLE_ADMIN"})
    void checkInBoardingPass_success() throws Exception {
        BoardingPass boardingPass = createBoardingPass(ticket);
        boardingPass.setStatus(BoardingPassStatus.GENERATED);
        boardingPass = boardingPassRepository.save(boardingPass);

        mockMvc.perform(post("/api/boarding-passes/" + boardingPass.getId() + "/check-in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value(BoardingPassStatus.CHECKED_IN.toString()));
    }

    @Test
    @WithMockUser(authorities = {"BOARDING_PASS_WRITE", "ROLE_ADMIN"})
    void boardPassenger_success() throws Exception {
        BoardingPass boardingPass = createBoardingPass(ticket);
        boardingPass.setStatus(BoardingPassStatus.BOARDING);
        boardingPass = boardingPassRepository.save(boardingPass);

        mockMvc.perform(post("/api/boarding-passes/" + boardingPass.getId() + "/board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value(BoardingPassStatus.USED.toString()));
    }

    @Test
    @WithMockUser(authorities = {"BOARDING_PASS_READ", "ROLE_ADMIN"})
    void generateQrCode_success() throws Exception {
        BoardingPass boardingPass = createBoardingPass(ticket);
        boardingPass.setVerificationToken("test-verification-token");
        boardingPass = boardingPassRepository.save(boardingPass);

        mockMvc.perform(get("/api/boarding-passes/" + boardingPass.getId() + "/qr-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.qrCode").isNotEmpty())
                .andExpect(jsonPath("$.data.format").value("PNG"))
                .andExpect(jsonPath("$.data.encoding").value("BASE64"));
    }

    @Test
    @WithMockUser(authorities = {"BOARDING_PASS_READ", "ROLE_ADMIN"})
    void verifyQrToken_success() throws Exception {
        BoardingPass boardingPass = createBoardingPass(ticket);
        boardingPass = boardingPassRepository.save(boardingPass);

        String realToken = qrTokenUtil.generateVerificationToken(
                boardingPass.getId(),
                boardingPass.getBoardingPassNumber(),
                boardingPass.getStatus().toString()
        );
        boardingPass.setVerificationToken(realToken);
        boardingPass = boardingPassRepository.save(boardingPass);

        String requestBody = "{\"token\":\"" + realToken + "\"}";

        mockMvc.perform(post("/api/boarding-passes/verify")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    @Test
    @WithMockUser(authorities = {"BOARDING_PASS_READ", "ROLE_ADMIN"})
    void verifyQrToken_missingToken() throws Exception {
        String requestBody = "{}";

        mockMvc.perform(post("/api/boarding-passes/verify")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest());
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
        aircraft.setRegistrationNumber("BP" + testCounter);
        aircraft.setType("COMMERCIAL");
        aircraft.setModel("Boeing 737");
        aircraft.setManufacturer("Boeing");
        aircraft.setTotalCapacity((short) 150);
        aircraft = aircraftRepository.save(aircraft);

        Flight flight = new Flight();
        flight.setFlightNumber("BP" + testCounter);
        flight.setOriginAirport(origin);
        flight.setDestinationAirport(destination);
        flight.setAircraft(aircraft);
        flight.setScheduledDeparture(Instant.now().plusSeconds(86400));
        flight.setScheduledArrival(Instant.now().plusSeconds(172800));
        flight.setStatus(FlightStatus.SCHEDULED);
        flight.setIsActive(true);
        flight.setTerminal("T1");
        flight.setGate("A1");
        flight = flightRepository.save(flight);
        this.createdFlight = flight;

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
        this.createdPassenger = passenger;
        this.createdAircraft = aircraft;

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingReference("BP" + testCounter);
        booking.setTotalAmount(BigDecimal.valueOf(200.00));
        booking.setCurrency("USD");
        booking.setBookingDate(Instant.now());
        booking.setPaymentStatus(BookingPaymentStatus.PAID);
        return bookingRepository.save(booking);
    }

    private Ticket createTicket(Booking booking, Flight flight, Passenger passenger, Aircraft aircraft) {
        Ticket ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setPassenger(passenger);
        ticket.setFlight(flight);
        ticket.setTicketNumber("TKT" + System.currentTimeMillis());
        ticket.setFareBasis("ECONOMY");
        ticket.setFare(BigDecimal.valueOf(100.00));
        ticket.setTaxes(BigDecimal.valueOf(20.00));
        ticket.setStatus(TicketStatus.ACTIVE);
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

    private BoardingPass createBoardingPass(Ticket ticket) {
        BoardingPass boardingPass = new BoardingPass();
        boardingPass.setBoardingPassNumber("BP" + System.currentTimeMillis());
        boardingPass.setTicket(ticket);
        boardingPass.setPassenger(ticket.getPassenger());
        boardingPass.setFlight(ticket.getFlight());
        boardingPass.setBooking(ticket.getBooking());
        boardingPass.setStatus(BoardingPassStatus.GENERATED);
        boardingPass.setGeneratedAt(Instant.now());
        boardingPass.setQrCodePayload("QR_PAYLOAD_PLACEHOLDER");
        boardingPass.setVersion(0L);
        return boardingPass;
    }
}
