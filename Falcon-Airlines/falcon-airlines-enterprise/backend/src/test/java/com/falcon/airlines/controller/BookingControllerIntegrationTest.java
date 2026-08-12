package com.falcon.airlines.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.dto.request.BookingRequest;
import com.falcon.airlines.dto.request.CancelBookingRequest;
import com.falcon.airlines.entity.Booking;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.entity.Seat;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.entity.Aircraft;
import com.falcon.airlines.enums.BookingStatus;
import com.falcon.airlines.enums.FlightStatus;
import com.falcon.airlines.repository.BookingRepository;
import com.falcon.airlines.repository.FlightRepository;
import com.falcon.airlines.repository.PassengerRepository;
import com.falcon.airlines.repository.SeatRepository;
import com.falcon.airlines.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class BookingControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.falcon.airlines.repository.AirportRepository airportRepository;

    @Autowired
    private com.falcon.airlines.repository.AircraftRepository aircraftRepository;

    private User customer;
    private Flight flight;
    private Passenger passenger;
    private Seat seat;
    private int testCounter = 0;

    @BeforeEach
    void setupTestData() {
        testCounter++;
        customer = createCustomer("ci_customer_" + testCounter);
        flight = createFlightWithAircraft("CI00" + testCounter, "REG_CI00" + testCounter);
        passenger = createPassenger("CI", "Passenger_" + testCounter, "PP_CI00" + testCounter);
        seat = createSeat(flight.getAircraft(), "1A", "ECONOMY");
    }

    // BOOKING CREATION TESTS

    @Test
    @WithMockUser(authorities = "BOOKING_WRITE")
    void createBookingSuccessfully() throws Exception {
        BookingRequest request = buildBookingRequest(customer.getId(), flight.getId(), passenger.getId(), List.of("1A"));

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.bookingReference").exists())
                .andExpect(jsonPath("$.data.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.data.flightId").value(flight.getId()));

        // Verify booking exists in database
        List<Booking> bookings = bookingRepository.findByCustomerId(customer.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getBookingReference()).isNotNull();
    }

    @Test
    @WithMockUser(authorities = "BOOKING_WRITE")
    void createBookingWithMissingPassenger() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setCustomerId(customer.getId());
        request.setFlightId(flight.getId());
        request.setRequestedSeats(List.of("1A"));
        request.setPassengers(List.of()); // Empty passengers

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "BOOKING_WRITE")
    void createBookingWithMissingFlight() throws Exception {
        BookingRequest request = buildBookingRequest(customer.getId(), 99999L, passenger.getId(), List.of("1A"));

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "BOOKING_WRITE")
    void createBookingWithInvalidSeat() throws Exception {
        BookingRequest request = buildBookingRequest(customer.getId(), flight.getId(), passenger.getId(), List.of("ZZ9"));

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "BOOKING_WRITE")
    void createBookingWithDuplicateSeats() throws Exception {
        BookingRequest request = buildBookingRequest(customer.getId(), flight.getId(), passenger.getId(), List.of("1A", "1A"));

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // BOOKING RETRIEVAL TESTS

    @Test
    @WithMockUser(authorities = "BOOKING_READ")
    void getBookingByIdSuccessfully() throws Exception {
        // Create a booking first
        Booking booking = createBooking(customer, "REF_CI001");
        Booking saved = bookingRepository.save(booking);

        mockMvc.perform(get("/api/bookings/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.bookingReference").value("REF_CI001"));
    }

    @Test
    @WithMockUser(authorities = "BOOKING_READ")
    void getBookingByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/bookings/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "BOOKING_READ")
    void getBookingByReferenceSuccessfully() throws Exception {
        Booking booking = createBooking(customer, "REF_CI002");
        bookingRepository.save(booking);

        mockMvc.perform(get("/api/bookings/reference/REF_CI002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingReference").value("REF_CI002"));
    }

    @Test
    @WithMockUser(authorities = "BOOKING_READ")
    void getBookingByReferenceNotFound() throws Exception {
        mockMvc.perform(get("/api/bookings/reference/NOTFOUND"))
                .andExpect(status().isNotFound());
    }

    // BOOKING UPDATE TESTS

    @Test
    @WithMockUser(authorities = "BOOKING_WRITE")
    void updateBookingSuccessfully() throws Exception {
        Booking booking = createBooking(customer, "REF_CI003");
        Booking saved = bookingRepository.save(booking);

        BookingRequest request = buildBookingRequest(customer.getId(), flight.getId(), passenger.getId(), List.of("1A"));

        mockMvc.perform(put("/api/bookings/" + saved.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(saved.getId()));
    }

    @Test
    @WithMockUser(authorities = "BOOKING_WRITE")
    void updateBookingNotFound() throws Exception {
        BookingRequest request = buildBookingRequest(customer.getId(), flight.getId(), passenger.getId(), List.of("1A"));

        mockMvc.perform(put("/api/bookings/99999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // BOOKING CANCELLATION TESTS

    @Test
    @WithMockUser(authorities = "BOOKING_WRITE")
    void cancelBookingSuccessfully() throws Exception {
        Booking booking = createBooking(customer, "REF_CI004");
        Booking saved = bookingRepository.save(booking);

        CancelBookingRequest request = new CancelBookingRequest();
        request.setCancellationReason("Customer request");

        mockMvc.perform(post("/api/bookings/" + saved.getId() + "/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify booking status changed
        Booking cancelled = bookingRepository.findById(saved.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @WithMockUser(authorities = "BOOKING_WRITE")
    void cancelBookingNotFound() throws Exception {
        CancelBookingRequest request = new CancelBookingRequest();
        request.setCancellationReason("Test");

        mockMvc.perform(post("/api/bookings/99999/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "BOOKING_WRITE")
    void cancelAlreadyCancelledBooking() throws Exception {
        Booking booking = createBooking(customer, "REF_CI005");
        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);

        CancelBookingRequest request = new CancelBookingRequest();
        request.setCancellationReason("Test");

        mockMvc.perform(post("/api/bookings/" + saved.getId() + "/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // BOOKING HISTORY TESTS

    @Test
    @WithMockUser(authorities = "BOOKING_READ")
    void getBookingHistorySuccessfully() throws Exception {
        // Create multiple bookings
        for (int i = 0; i < 3; i++) {
            Booking booking = createBooking(customer, "REF_CI00" + i);
            bookingRepository.save(booking);
        }

        mockMvc.perform(get("/api/bookings/history")
                        .param("customerId", String.valueOf(customer.getId()))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.data.totalBookings").value(3));
    }

    @Test
    @WithMockUser(authorities = "BOOKING_READ")
    void getBookingHistoryWithPagination() throws Exception {
        for (int i = 0; i < 15; i++) {
            Booking booking = createBooking(customer, "REF_CI01" + i);
            bookingRepository.save(booking);
        }

        mockMvc.perform(get("/api/bookings/history")
                        .param("customerId", String.valueOf(customer.getId()))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBookings").value(15));
    }

    // SEAT AVAILABILITY TESTS

    @Test
    @WithMockUser(authorities = "BOOKING_READ")
    void checkSeatAvailabilitySuccessfully() throws Exception {
        mockMvc.perform(get("/api/bookings/seats/availability")
                        .param("flightId", String.valueOf(flight.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.flightId").value(flight.getId()))
                .andExpect(jsonPath("$.data.flightNumber").value(flight.getFlightNumber()))
                .andExpect(jsonPath("$.data.totalSeats").exists())
                .andExpect(jsonPath("$.data.availableSeats").exists());
    }

    @Test
    @WithMockUser(authorities = "BOOKING_READ")
    void checkSeatAvailabilityFlightNotFound() throws Exception {
        mockMvc.perform(get("/api/bookings/seats/availability")
                        .param("flightId", "99999"))
                .andExpect(status().isNotFound());
    }

    // DUPLICATE ALLOCATION TESTS

    @Test
    @WithMockUser(authorities = "BOOKING_WRITE")
    void duplicateSeatAllocationRejected() throws Exception {
        // Create first booking
        BookingRequest request1 = buildBookingRequest(customer.getId(), flight.getId(), passenger.getId(), List.of("1A"));
        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Try to create second booking with same seat
        User customer2 = createCustomer("ci_customer2");
        Passenger passenger2 = createPassenger("CI2", "Passenger2", "PP_CI002");
        BookingRequest request2 = buildBookingRequest(customer2.getId(), flight.getId(), passenger2.getId(), List.of("1A"));

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    // SECURITY TESTS

    @Test
    void unauthenticatedRequestForbidden() throws Exception {
        BookingRequest request = buildBookingRequest(customer.getId(), flight.getId(), passenger.getId(), List.of("1A"));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PASSENGER_READ")
    void unauthorizedRoleForbidden() throws Exception {
        BookingRequest request = buildBookingRequest(customer.getId(), flight.getId(), passenger.getId(), List.of("1A"));

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "BOOKING_WRITE")
    void authorizedRoleAllowed() throws Exception {
        BookingRequest request = buildBookingRequest(customer.getId(), flight.getId(), passenger.getId(), List.of("1A"));

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    // INVALID BOOKING STATE TESTS

    @Test
    @WithMockUser(authorities = "BOOKING_WRITE")
    void updateCompletedBookingRejected() throws Exception {
        Booking booking = createBooking(customer, "REF_CI006");
        booking.setStatus(BookingStatus.COMPLETED);
        Booking saved = bookingRepository.save(booking);

        BookingRequest request = buildBookingRequest(customer.getId(), flight.getId(), passenger.getId(), List.of("1A"));

        mockMvc.perform(put("/api/bookings/" + saved.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // Helper methods

    private BookingRequest buildBookingRequest(Long customerId, Long flightId, Long passengerId, List<String> seats) {
        BookingRequest request = new BookingRequest();
        request.setCustomerId(customerId);
        request.setFlightId(flightId);
        request.setRequestedSeats(seats);

        BookingRequest.BookingPassengerRequest bp = new BookingRequest.BookingPassengerRequest();
        bp.setPassengerId(passengerId);
        bp.setFareClass("ECONOMY");

        request.setPassengers(List.of(bp));
        return request;
    }

    private User createCustomer(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hashed_password");
        user.setStatus(com.falcon.airlines.enums.UserStatus.ACTIVE);
        user.setMfaEnabled(false);
        user.setEmailVerified(false);
        return userRepository.save(user);
    }

    private Aircraft createAircraft(String registration) {
        Aircraft aircraft = new Aircraft();
        aircraft.setRegistrationNumber(registration);
        aircraft.setType("COMMERCIAL");
        aircraft.setModel("Boeing 737");
        aircraft.setManufacturer("Boeing");
        aircraft.setTotalCapacity((short) 150);
        return aircraft;
    }

    private Flight createFlightWithAircraft(String flightNumber, String aircraftReg) {
        Aircraft aircraft = createAircraft(aircraftReg);
        aircraft = aircraftRepository.save(aircraft);
        
        // Use same airports for all tests - @Transactional will rollback after each test
        com.falcon.airlines.entity.Airport origin = airportRepository.findByIataCode("JFK")
            .orElseGet(() -> {
                com.falcon.airlines.entity.Airport a = new com.falcon.airlines.entity.Airport();
                a.setIataCode("JFK");
                a.setIcaoCode("KJFK");
                a.setName("John F. Kennedy International");
                a.setCity("New York");
                a.setCountry("US");
                a.setTimeZone("America/New_York");
                a.setIsActive(true);
                return airportRepository.save(a);
            });
        
        com.falcon.airlines.entity.Airport destination = airportRepository.findByIataCode("LAX")
            .orElseGet(() -> {
                com.falcon.airlines.entity.Airport a = new com.falcon.airlines.entity.Airport();
                a.setIataCode("LAX");
                a.setIcaoCode("KLAX");
                a.setName("Los Angeles International");
                a.setCity("Los Angeles");
                a.setCountry("US");
                a.setTimeZone("America/Los_Angeles");
                a.setIsActive(true);
                return airportRepository.save(a);
            });

        Flight flight = new Flight();
        flight.setFlightNumber(flightNumber);
        flight.setOriginAirport(origin);
        flight.setDestinationAirport(destination);
        flight.setAircraft(aircraft);
        flight.setScheduledDeparture(Instant.now().plusSeconds(86400));
        flight.setScheduledArrival(Instant.now().plusSeconds(172800));
        flight.setStatus(FlightStatus.SCHEDULED);
        flight.setIsActive(true);
        flight.setTerminal("T1");
        flight.setGate("A1");
        
        return flightRepository.save(flight);
    }

    private Passenger createPassenger(String firstName, String lastName, String passportNumber) {
        Passenger passenger = new Passenger();
        passenger.setFirstName(firstName);
        passenger.setLastName(lastName);
        passenger.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        passenger.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
        passenger.setPhone("+1234567890");
        passenger.setPassportNumber(passportNumber);
        passenger.setNationality("USA");
        passenger.setGender(com.falcon.airlines.enums.Gender.M);
        return passengerRepository.save(passenger);
    }

    private Seat createSeat(Aircraft aircraft, String seatNumber, String seatClass) {
        Seat seat = new Seat();
        seat.setAircraft(aircraft);
        seat.setSeatNumber(seatNumber);
        seat.setSeatClass(seatClass);
        seat.setRowNumber((short) 1);
        seat.setColumnLetter("A");
        seat.setIsActive(true);
        Seat savedSeat = seatRepository.save(seat);
        // Verify seat can be found by the same query used in BookingService
        Seat foundSeat = seatRepository.findByAircraftIdAndSeatNumber(aircraft.getId(), seatNumber)
            .orElse(null);
        assertThat(foundSeat).isNotNull();
        return savedSeat;
    }

    private Booking createBooking(User customer, String reference) {
        Booking booking = new Booking();
        booking.setBookingReference(reference);
        booking.setCustomer(customer);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(java.math.BigDecimal.valueOf(100.00));
        booking.setCurrency("USD");
        booking.setBookingDate(Instant.now());
        booking.setPaymentStatus(com.falcon.airlines.enums.BookingPaymentStatus.PENDING);
        return booking;
    }
}
