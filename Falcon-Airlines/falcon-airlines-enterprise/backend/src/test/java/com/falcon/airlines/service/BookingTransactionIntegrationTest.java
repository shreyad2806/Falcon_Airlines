package com.falcon.airlines.service;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.dto.request.BookingRequest;
import com.falcon.airlines.dto.response.BookingResponse;
import com.falcon.airlines.entity.Booking;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.entity.Seat;
import com.falcon.airlines.entity.SeatAllocation;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.entity.Aircraft;
import com.falcon.airlines.enums.BookingStatus;
import com.falcon.airlines.enums.BookingPaymentStatus;
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
import com.falcon.airlines.repository.AircraftRepository;
import com.falcon.airlines.repository.AirportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class BookingTransactionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Test
    void successfulBookingCommitsAllChanges() {
        // Setup test data
        User customer = createCustomer("tx_cust1");
        Flight flight = createFlightWithAircraft("FL001", "REG001");
        Passenger passenger1 = createPassenger("John", "Doe", "PP123456");
        Passenger passenger2 = createPassenger("Jane", "Smith", "PP789012");
        Seat seat1 = createSeat(flight.getAircraft(), "1A", "ECONOMY");
        Seat seat2 = createSeat(flight.getAircraft(), "1B", "ECONOMY");

        // Create booking request
        BookingRequest request = new BookingRequest();
        request.setCustomerId(customer.getId());
        request.setFlightId(flight.getId());
        request.setRequestedSeats(List.of("1A", "1B"));

        BookingRequest.BookingPassengerRequest bp1 = new BookingRequest.BookingPassengerRequest();
        bp1.setPassengerId(passenger1.getId());
        bp1.setFareClass("ECONOMY");

        BookingRequest.BookingPassengerRequest bp2 = new BookingRequest.BookingPassengerRequest();
        bp2.setPassengerId(passenger2.getId());
        bp2.setFareClass("ECONOMY");

        request.setPassengers(List.of(bp1, bp2));

        // Execute booking
        BookingResponse response = bookingService.createBooking(request);

        // Verify booking exists
        Optional<Booking> bookingOpt = bookingRepository.findById(response.getId());
        assertThat(bookingOpt).isPresent();
        Booking booking = bookingOpt.get();
        assertThat(booking.getBookingReference()).isEqualTo(response.getBookingReference());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getCustomer().getId()).isEqualTo(customer.getId());

        // Verify tickets exist
        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        assertThat(tickets).hasSize(2);
        assertThat(tickets).allMatch(t -> t.getStatus() == TicketStatus.ACTIVE);

        // Verify seat allocations exist
        Optional<SeatAllocation> alloc1 = seatAllocationRepository.findBySeatIdAndFlightId(seat1.getId(), flight.getId());
        Optional<SeatAllocation> alloc2 = seatAllocationRepository.findBySeatIdAndFlightId(seat2.getId(), flight.getId());
        assertThat(alloc1).isPresent();
        assertThat(alloc2).isPresent();
    }

    @Test
    void failureDuringBookingRollsBackAllChanges() {
        // Setup test data
        User customer = createCustomer("tx_cust2");
        Flight flight = createFlightWithAircraft("FL002", "REG002");
        Passenger passenger1 = createPassenger("Bob", "Jones", "PP345678");
        Seat seat1 = createSeat(flight.getAircraft(), "1A", "ECONOMY");

        // Create booking request with non-existent passenger to trigger failure
        BookingRequest request = new BookingRequest();
        request.setCustomerId(customer.getId());
        request.setFlightId(flight.getId());
        request.setRequestedSeats(List.of("1A"));

        BookingRequest.BookingPassengerRequest bp1 = new BookingRequest.BookingPassengerRequest();
        bp1.setPassengerId(99999L); // Non-existent passenger
        bp1.setFareClass("ECONOMY");

        request.setPassengers(List.of(bp1));

        // Execute booking and expect failure
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BaseException.class);

        // Verify no booking was created
        List<Booking> customerBookings = bookingRepository.findByCustomerId(customer.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        assertThat(customerBookings).isEmpty();

        // Verify no tickets were created
        List<Ticket> allTickets = ticketRepository.findAll();
        assertThat(allTickets).isEmpty();

        // Verify seat remains available
        Optional<SeatAllocation> allocation = seatAllocationRepository.findBySeatIdAndFlightId(seat1.getId(), flight.getId());
        assertThat(allocation).isEmpty();
    }

    @Test
    void bookingAlreadyOccupiedSeatIsRejected() {
        // Setup test data
        User customer1 = createCustomer("tx_cust3");
        User customer2 = createCustomer("tx_cust4");
        Flight flight = createFlightWithAircraft("FL003", "REG003");
        Passenger passenger1 = createPassenger("Alice", "Brown", "PP456789");
        Passenger passenger2 = createPassenger("Charlie", "White", "PP567890");
        Seat seat1 = createSeat(flight.getAircraft(), "1A", "ECONOMY");

        // Create first booking
        BookingRequest request1 = new BookingRequest();
        request1.setCustomerId(customer1.getId());
        request1.setFlightId(flight.getId());
        request1.setRequestedSeats(List.of("1A"));

        BookingRequest.BookingPassengerRequest bp1 = new BookingRequest.BookingPassengerRequest();
        bp1.setPassengerId(passenger1.getId());
        bp1.setFareClass("ECONOMY");

        request1.setPassengers(List.of(bp1));

        bookingService.createBooking(request1);

        // Verify seat is now allocated
        Optional<SeatAllocation> firstAlloc = seatAllocationRepository.findBySeatIdAndFlightId(seat1.getId(), flight.getId());
        assertThat(firstAlloc).isPresent();

        // Try to create second booking with same seat
        BookingRequest request2 = new BookingRequest();
        request2.setCustomerId(customer2.getId());
        request2.setFlightId(flight.getId());
        request2.setRequestedSeats(List.of("1A"));

        BookingRequest.BookingPassengerRequest bp2 = new BookingRequest.BookingPassengerRequest();
        bp2.setPassengerId(passenger2.getId());
        bp2.setFareClass("ECONOMY");

        request2.setPassengers(List.of(bp2));

        // Should fail with conflict
        assertThatThrownBy(() -> bookingService.createBooking(request2))
                .isInstanceOf(BaseException.class);

        // Verify only one booking exists for customer2
        List<Booking> customer2Bookings = bookingRepository.findByCustomerId(customer2.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        assertThat(customer2Bookings).isEmpty();

        // Verify seat allocation still belongs to first booking
        Optional<SeatAllocation> allocAfter = seatAllocationRepository.findBySeatIdAndFlightId(seat1.getId(), flight.getId());
        assertThat(allocAfter).isPresent();
        assertThat(allocAfter.get().getTicket().getBooking().getCustomer().getId()).isEqualTo(customer1.getId());
    }

    @Test
    void cancellationCommitsAtomically() {
        // Setup test data
        User customer = createCustomer("tx_cust5");
        Flight flight = createFlightWithAircraft("FL004", "REG004");
        Passenger passenger = createPassenger("David", "Lee", "PP678901");
        Seat seat1 = createSeat(flight.getAircraft(), "1A", "ECONOMY");

        // Create booking
        BookingRequest request = new BookingRequest();
        request.setCustomerId(customer.getId());
        request.setFlightId(flight.getId());
        request.setRequestedSeats(List.of("1A"));

        BookingRequest.BookingPassengerRequest bp = new BookingRequest.BookingPassengerRequest();
        bp.setPassengerId(passenger.getId());
        bp.setFareClass("ECONOMY");

        request.setPassengers(List.of(bp));

        BookingResponse bookingResponse = bookingService.createBooking(request);
        Long bookingId = bookingResponse.getId();

        // Verify initial state
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);

        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getStatus()).isEqualTo(TicketStatus.ACTIVE);

        Optional<SeatAllocation> allocation = seatAllocationRepository.findBySeatIdAndFlightId(seat1.getId(), flight.getId());
        assertThat(allocation).isPresent();

        // Cancel booking
        bookingService.cancelBooking(bookingId, "Customer request");

        // Verify booking status changed
        Booking cancelledBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(cancelledBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        // Verify ticket status changed
        List<Ticket> cancelledTickets = ticketRepository.findByBookingId(bookingId);
        assertThat(cancelledTickets).hasSize(1);
        assertThat(cancelledTickets.get(0).getStatus()).isEqualTo(TicketStatus.CANCELLED);

        // Verify seat released
        Optional<SeatAllocation> allocationAfter = seatAllocationRepository.findBySeatIdAndFlightId(seat1.getId(), flight.getId());
        assertThat(allocationAfter).isEmpty();
    }

    @Test
    void cancellationOfNonExistentBookingFailsWithoutSideEffects() {
        // Setup test data
        Flight flight = createFlightWithAircraft("FL005", "REG005");
        Passenger passenger = createPassenger("Eve", "Green", "PP789012");
        Seat seat1 = createSeat(flight.getAircraft(), "1A", "ECONOMY");

        // Create a valid booking first
        User customer = createCustomer("tx_cust6");
        BookingRequest request = new BookingRequest();
        request.setCustomerId(customer.getId());
        request.setFlightId(flight.getId());
        request.setRequestedSeats(List.of("1A"));

        BookingRequest.BookingPassengerRequest bp = new BookingRequest.BookingPassengerRequest();
        bp.setPassengerId(passenger.getId());
        bp.setFareClass("ECONOMY");

        request.setPassengers(List.of(bp));

        BookingResponse bookingResponse = bookingService.createBooking(request);
        Long validBookingId = bookingResponse.getId();

        // Try to cancel non-existent booking
        assertThatThrownBy(() -> bookingService.cancelBooking(99999L, "Test"))
                .isInstanceOf(BaseException.class);

        // Verify valid booking is still intact
        Booking validBooking = bookingRepository.findById(validBookingId).orElseThrow();
        assertThat(validBooking.getStatus()).isEqualTo(BookingStatus.PENDING);

        List<Ticket> tickets = ticketRepository.findByBookingId(validBookingId);
        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getStatus()).isEqualTo(TicketStatus.ACTIVE);

        Optional<SeatAllocation> allocation = seatAllocationRepository.findBySeatIdAndFlightId(seat1.getId(), flight.getId());
        assertThat(allocation).isPresent();
    }

    @Test
    void invalidPassengerCausesNoPartialData() {
        // Setup test data
        User customer = createCustomer("tx_cust7");
        Flight flight = createFlightWithAircraft("FL006", "REG006");
        Seat seat1 = createSeat(flight.getAircraft(), "1A", "ECONOMY");

        // Create booking request with non-existent passenger
        BookingRequest request = new BookingRequest();
        request.setCustomerId(customer.getId());
        request.setFlightId(flight.getId());
        request.setRequestedSeats(List.of("1A"));

        BookingRequest.BookingPassengerRequest bp = new BookingRequest.BookingPassengerRequest();
        bp.setPassengerId(99999L); // Non-existent
        bp.setFareClass("ECONOMY");

        request.setPassengers(List.of(bp));

        // Should fail
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BaseException.class);

        // Verify no partial data
        List<Booking> bookings = bookingRepository.findByCustomerId(customer.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        assertThat(bookings).isEmpty();

        List<Ticket> tickets = ticketRepository.findAll();
        assertThat(tickets).isEmpty();

        Optional<SeatAllocation> allocation = seatAllocationRepository.findBySeatIdAndFlightId(seat1.getId(), flight.getId());
        assertThat(allocation).isEmpty();
    }

    @Test
    void invalidFlightCausesNoPartialData() {
        // Setup test data
        User customer = createCustomer("tx_cust8");
        Passenger passenger = createPassenger("Frank", "Miller", "PP890123");

        // Create booking request with non-existent flight
        BookingRequest request = new BookingRequest();
        request.setCustomerId(customer.getId());
        request.setFlightId(99999L); // Non-existent flight
        request.setRequestedSeats(List.of("1A"));

        BookingRequest.BookingPassengerRequest bp = new BookingRequest.BookingPassengerRequest();
        bp.setPassengerId(passenger.getId());
        bp.setFareClass("ECONOMY");

        request.setPassengers(List.of(bp));

        // Should fail
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BaseException.class);

        // Verify no partial data
        List<Booking> bookings = bookingRepository.findByCustomerId(customer.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        assertThat(bookings).isEmpty();
    }

    @Test
    void invalidSeatCausesNoPartialData() {
        // Setup test data
        User customer = createCustomer("tx_cust9");
        Flight flight = createFlightWithAircraft("FL007", "REG007");
        Passenger passenger = createPassenger("Grace", "Wilson", "PP901234");

        // Create booking request with non-existent seat
        BookingRequest request = new BookingRequest();
        request.setCustomerId(customer.getId());
        request.setFlightId(flight.getId());
        request.setRequestedSeats(List.of("ZZ9")); // Non-existent seat

        BookingRequest.BookingPassengerRequest bp = new BookingRequest.BookingPassengerRequest();
        bp.setPassengerId(passenger.getId());
        bp.setFareClass("ECONOMY");

        request.setPassengers(List.of(bp));

        // Should fail
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BaseException.class);

        // Verify no partial data
        List<Booking> bookings = bookingRepository.findByCustomerId(customer.getId(), 
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        assertThat(bookings).isEmpty();

        List<Ticket> tickets = ticketRepository.findAll();
        assertThat(tickets).isEmpty();
    }

    // Helper methods to create test data

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
        
        // Create origin and destination airports
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
        return seatRepository.save(seat);
    }
}
