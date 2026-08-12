package com.falcon.airlines.repository;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.entity.Aircraft;
import com.falcon.airlines.entity.Airport;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.Seat;
import com.falcon.airlines.entity.SeatAllocation;
import com.falcon.airlines.entity.Ticket;
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
        Ticket ticket1 = createTicket();
        Ticket ticket2 = createTicket();
        
        SeatAllocation allocation1 = new SeatAllocation();
        allocation1.setSeat(seat);
        allocation1.setTicket(ticket1);
        allocation1.setFlight(flight);
        seatAllocationRepository.save(allocation1);

        // Attempt to allocate same seat to same flight with different ticket
        SeatAllocation allocation2 = new SeatAllocation();
        allocation2.setSeat(seat);
        allocation2.setTicket(ticket2);
        allocation2.setFlight(flight);

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
        Seat seat1 = createSeat(aircraft);
        seat1.setSeatNumber("1A");
        Seat seat2 = createSeat(aircraft);
        seat2.setSeatNumber("1B");
        seatRepository.save(seat1);
        seatRepository.save(seat2);
        
        Ticket ticket = createTicket();
        
        SeatAllocation allocation1 = new SeatAllocation();
        allocation1.setSeat(seat1);
        allocation1.setTicket(ticket);
        allocation1.setFlight(flight);
        seatAllocationRepository.save(allocation1);

        // Attempt to assign same ticket to different seat
        SeatAllocation allocation2 = new SeatAllocation();
        allocation2.setSeat(seat2);
        allocation2.setTicket(ticket);
        allocation2.setFlight(flight);

        // This should fail due to unique constraint uk_seat_allocations_ticket
        try {
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
        aircraft.setRegistrationNumber("TEST001");
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
        Ticket ticket = new Ticket();
        ticket.setTicketNumber("TKT" + System.currentTimeMillis());
        ticket.setFare(java.math.BigDecimal.valueOf(100.00));
        ticket.setTaxes(java.math.BigDecimal.valueOf(20.00));
        return ticketRepository.save(ticket);
    }
}
