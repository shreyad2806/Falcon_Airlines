package com.falcon.airlines.repository;

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
import com.falcon.airlines.enums.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SeatAllocationRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindSeatAllocation() {
        Aircraft aircraft = createAircraft();
        Flight flight = createFlight(aircraft);
        Seat seat = createSeat(aircraft);
        Ticket ticket = createTicket();
        
        SeatAllocation allocation = new SeatAllocation();
        allocation.setSeat(seat);
        allocation.setTicket(ticket);
        allocation.setFlight(flight);
        allocation.setAllocatedAt(java.time.Instant.now());
        
        SeatAllocation saved = seatAllocationRepository.save(allocation);
        assertThat(saved.getId()).isNotNull();

        Optional<SeatAllocation> found = seatAllocationRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getSeat().getSeatNumber()).isEqualTo("1A");
    }

    @Test
    void findBySeatIdAndFlightId() {
        Aircraft aircraft = createAircraft();
        Flight flight = createFlight(aircraft);
        Seat seat = createSeat(aircraft);
        Ticket ticket = createTicket();
        
        SeatAllocation allocation = new SeatAllocation();
        allocation.setSeat(seat);
        allocation.setTicket(ticket);
        allocation.setFlight(flight);
        allocation.setAllocatedAt(java.time.Instant.now());
        SeatAllocation saved = seatAllocationRepository.save(allocation);

        Optional<SeatAllocation> found = seatAllocationRepository.findBySeatIdAndFlightId(seat.getId(), flight.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findBySeatIdAndFlightId_notFound() {
        Optional<SeatAllocation> found = seatAllocationRepository.findBySeatIdAndFlightId(999L, 999L);
        assertThat(found).isEmpty();
    }

    @Test
    void findByTicketId() {
        Aircraft aircraft = createAircraft();
        Flight flight = createFlight(aircraft);
        Seat seat = createSeat(aircraft);
        Ticket ticket = createTicket();
        
        SeatAllocation allocation = new SeatAllocation();
        allocation.setSeat(seat);
        allocation.setTicket(ticket);
        allocation.setFlight(flight);
        allocation.setAllocatedAt(java.time.Instant.now());
        SeatAllocation saved = seatAllocationRepository.save(allocation);

        Optional<SeatAllocation> found = seatAllocationRepository.findByTicketId(ticket.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByTicketId_notFound() {
        Optional<SeatAllocation> found = seatAllocationRepository.findByTicketId(999L);
        assertThat(found).isEmpty();
    }

    @Test
    void existsBySeatIdAndFlightId() {
        Aircraft aircraft = createAircraft();
        Flight flight = createFlight(aircraft);
        Seat seat = createSeat(aircraft);
        Ticket ticket = createTicket();
        
        SeatAllocation allocation = new SeatAllocation();
        allocation.setSeat(seat);
        allocation.setTicket(ticket);
        allocation.setFlight(flight);
        allocation.setAllocatedAt(java.time.Instant.now());
        seatAllocationRepository.save(allocation);

        boolean exists = seatAllocationRepository.existsBySeatIdAndFlightId(seat.getId(), flight.getId());
        assertThat(exists).isTrue();
    }

    @Test
    void existsBySeatIdAndFlightId_notExists() {
        boolean exists = seatAllocationRepository.existsBySeatIdAndFlightId(999L, 999L);
        assertThat(exists).isFalse();
    }

    @Test
    void existsByTicketId() {
        Aircraft aircraft = createAircraft();
        Flight flight = createFlight(aircraft);
        Seat seat = createSeat(aircraft);
        Ticket ticket = createTicket();
        
        SeatAllocation allocation = new SeatAllocation();
        allocation.setSeat(seat);
        allocation.setTicket(ticket);
        allocation.setFlight(flight);
        allocation.setAllocatedAt(java.time.Instant.now());
        seatAllocationRepository.save(allocation);

        boolean exists = seatAllocationRepository.existsByTicketId(ticket.getId());
        assertThat(exists).isTrue();
    }

    @Test
    void existsByTicketId_notExists() {
        boolean exists = seatAllocationRepository.existsByTicketId(999L);
        assertThat(exists).isFalse();
    }

    @Test
    void duplicateSeatAllocationPrevented() {
        Aircraft aircraft = createAircraft();
        Flight flight = createFlight(aircraft);
        Seat seat = createSeat(aircraft);
        
        // Create tickets using the same aircraft
        Booking booking1 = createBooking();
        Passenger passenger1 = createPassenger();
        Ticket ticket1 = new Ticket();
        ticket1.setTicketNumber("TKT1" + System.currentTimeMillis());
        ticket1.setBooking(booking1);
        ticket1.setPassenger(passenger1);
        ticket1.setFlight(flight);
        ticket1.setFare(java.math.BigDecimal.valueOf(100.00));
        ticket1.setFareBasis("Y");
        ticket1.setTaxes(java.math.BigDecimal.valueOf(20.00));
        ticket1.setStatus(TicketStatus.ISSUED);
        ticket1.setIssuedAt(java.time.Instant.now());
        ticket1 = ticketRepository.save(ticket1);
        
        Booking booking2 = createBooking();
        Passenger passenger2 = createPassenger();
        Ticket ticket2 = new Ticket();
        ticket2.setTicketNumber("TKT2" + System.currentTimeMillis());
        ticket2.setBooking(booking2);
        ticket2.setPassenger(passenger2);
        ticket2.setFlight(flight);
        ticket2.setFare(java.math.BigDecimal.valueOf(100.00));
        ticket2.setFareBasis("Y");
        ticket2.setTaxes(java.math.BigDecimal.valueOf(20.00));
        ticket2.setStatus(TicketStatus.ISSUED);
        ticket2.setIssuedAt(java.time.Instant.now());
        ticket2 = ticketRepository.save(ticket2);
        
        SeatAllocation allocation1 = new SeatAllocation();
        allocation1.setSeat(seat);
        allocation1.setTicket(ticket1);
        allocation1.setFlight(flight);
        allocation1.setAllocatedAt(java.time.Instant.now());
        seatAllocationRepository.save(allocation1);

        // Attempt to allocate same seat to same flight with different ticket
        SeatAllocation allocation2 = new SeatAllocation();
        allocation2.setSeat(seat);
        allocation2.setTicket(ticket2);
        allocation2.setFlight(flight);
        allocation2.setAllocatedAt(java.time.Instant.now());

        // This should fail due to unique constraint uk_seat_allocations_seat_flight
        try {
            seatAllocationRepository.save(allocation2);
            // If we get here, the test should fail
            assertThat(false).isTrue();
        } catch (Exception e) {
            // Expected - unique constraint violation
            assertThat(e).isNotNull();
        }
    }

    @Test
    void duplicateTicketAssignmentPrevented() {
        Aircraft aircraft = createAircraft();
        Flight flight = createFlight(aircraft);
        
        Seat seat1 = new Seat();
        seat1.setAircraft(aircraft);
        seat1.setSeatNumber("1A");
        seat1.setSeatClass("ECONOMY");
        seat1.setRowNumber((short) 1);
        seat1.setColumnLetter("A");
        seat1.setIsActive(true);
        seat1 = seatRepository.save(seat1);
        
        Seat seat2 = new Seat();
        seat2.setAircraft(aircraft);
        seat2.setSeatNumber("1B");
        seat2.setSeatClass("ECONOMY");
        seat2.setRowNumber((short) 1);
        seat2.setColumnLetter("B");
        seat2.setIsActive(true);
        seat2 = seatRepository.save(seat2);
        
        Ticket ticket = createTicket();
        
        SeatAllocation allocation1 = new SeatAllocation();
        allocation1.setSeat(seat1);
        allocation1.setTicket(ticket);
        allocation1.setFlight(flight);
        allocation1.setAllocatedAt(java.time.Instant.now());
        seatAllocationRepository.save(allocation1);

        try {
            SeatAllocation allocation2 = new SeatAllocation();
            allocation2.setSeat(seat2);
            allocation2.setTicket(ticket);
            allocation2.setFlight(flight);
            allocation2.setAllocatedAt(java.time.Instant.now());
            seatAllocationRepository.save(allocation2);
            
            // If we get here, the test should fail
            assertThat(false).isTrue();
        } catch (Exception e) {
            // Expected - unique constraint violation
            assertThat(e).isNotNull();
        }
    }

    private Aircraft createAircraft() {
        Aircraft aircraft = new Aircraft();
        aircraft.setRegistrationNumber("TEST" + System.currentTimeMillis());
        aircraft.setType("BOEING");
        aircraft.setModel("737-800");
        aircraft.setManufacturer("Boeing");
        aircraft.setTotalCapacity((short) 180);
        return aircraftRepository.save(aircraft);
    }

    private Flight createFlight(Aircraft aircraft) {
        Airport origin = createAirport("JFK", "John F. Kennedy International");
        Airport destination = createAirport("LAX", "Los Angeles International");
        
        Flight flight = new Flight();
        flight.setFlightNumber("FL001");
        flight.setOriginAirport(origin);
        flight.setDestinationAirport(destination);
        flight.setAircraft(aircraft);
        flight.setScheduledDeparture(java.time.Instant.now().plusSeconds(3600));
        flight.setScheduledArrival(java.time.Instant.now().plusSeconds(7200));
        flight.setStatus(com.falcon.airlines.enums.FlightStatus.SCHEDULED);
        flight.setIsActive(true);
        return flightRepository.save(flight);
    }

    private Airport createAirport(String iataCode, String name) {
        return airportRepository.findByIataCode(iataCode)
            .orElseGet(() -> {
                Airport airport = new Airport();
                airport.setIataCode(iataCode);
                airport.setIcaoCode("K" + iataCode);
                airport.setName(name);
                airport.setCity("Test City");
                airport.setCountry("US");
                airport.setTimeZone("UTC");
                airport.setIsActive(true);
                return airportRepository.save(airport);
            });
    }

    private Seat createSeat(Aircraft aircraft) {
        Seat seat = new Seat();
        seat.setAircraft(aircraft);
        seat.setSeatNumber("1A");
        seat.setSeatClass("ECONOMY");
        seat.setRowNumber((short) 1);
        seat.setColumnLetter("A");
        seat.setIsActive(true);
        return seatRepository.save(seat);
    }

    private Ticket createTicket() {
        Booking booking = createBooking();
        Passenger passenger = createPassenger();
        Aircraft aircraft = createAircraft();
        Flight flight = createFlight(aircraft);
        
        Ticket ticket = new Ticket();
        ticket.setTicketNumber("TKT" + System.currentTimeMillis());
        ticket.setBooking(booking);
        ticket.setPassenger(passenger);
        ticket.setFlight(flight);
        ticket.setFare(java.math.BigDecimal.valueOf(100.00));
        ticket.setFareBasis("Y");
        ticket.setTaxes(java.math.BigDecimal.valueOf(20.00));
        ticket.setStatus(TicketStatus.ISSUED);
        ticket.setIssuedAt(java.time.Instant.now());
        return ticketRepository.save(ticket);
    }

    private Booking createBooking() {
        User customer = new User();
        customer.setUsername("cust_test_" + System.currentTimeMillis());
        customer.setEmail("cust_test_" + System.currentTimeMillis() + "@example.com");
        customer.setPasswordHash("hashed_password");
        customer.setStatus(com.falcon.airlines.enums.UserStatus.ACTIVE);
        customer.setMfaEnabled(false);
        customer.setEmailVerified(false);
        User savedCustomer = userRepository.save(customer);

        Booking booking = new Booking();
        booking.setBookingReference("REF" + (System.currentTimeMillis() % 10000000));
        booking.setCustomer(savedCustomer);
        booking.setStatus(com.falcon.airlines.enums.BookingStatus.PENDING);
        booking.setTotalAmount(java.math.BigDecimal.valueOf(100.00));
        booking.setCurrency("USD");
        booking.setBookingDate(java.time.Instant.now());
        booking.setPaymentStatus(com.falcon.airlines.enums.BookingPaymentStatus.PENDING);
        return bookingRepository.save(booking);
    }

    private Passenger createPassenger() {
        Passenger passenger = new Passenger();
        passenger.setFirstName("John");
        passenger.setLastName("Doe");
        passenger.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        passenger.setEmail("john.doe_" + System.currentTimeMillis() + "@example.com");
        passenger.setPhone("+1234567890");
        passenger.setPassportNumber("AB" + (System.currentTimeMillis() % 10000000));
        passenger.setNationality("USA");
        passenger.setGender(com.falcon.airlines.enums.Gender.M);
        return passengerRepository.save(passenger);
    }
}
