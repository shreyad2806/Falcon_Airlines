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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SeatRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Test
    void saveAndFindSeat() {
        Aircraft aircraft = createAircraft();
        Seat seat = createSeat(aircraft, "1A", "ECONOMY");
        
        Seat saved = seatRepository.save(seat);
        assertThat(saved.getId()).isNotNull();

        Optional<Seat> found = seatRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getSeatNumber()).isEqualTo("1A");
    }

    @Test
    void findByAircraftId() {
        Aircraft aircraft = createAircraft();
        Seat seat1 = createSeat(aircraft, "1A", "ECONOMY");
        Seat seat2 = createSeat(aircraft, "1B", "ECONOMY");
        seatRepository.save(seat1);
        seatRepository.save(seat2);

        List<Seat> seats = seatRepository.findByAircraftId(aircraft.getId());
        assertThat(seats).hasSize(2);
    }

    @Test
    void findByAircraftIdAndIsActiveTrue() {
        Aircraft aircraft = createAircraft();
        Seat seat1 = createSeat(aircraft, "1A", "ECONOMY");
        seat1.setIsActive(true);
        Seat seat2 = createSeat(aircraft, "1B", "ECONOMY");
        seat2.setIsActive(false);
        seatRepository.save(seat1);
        seatRepository.save(seat2);

        List<Seat> activeSeats = seatRepository.findByAircraftIdAndIsActiveTrue(aircraft.getId());
        assertThat(activeSeats).hasSize(1);
        assertThat(activeSeats.get(0).getSeatNumber()).isEqualTo("1A");
    }

    @Test
    void findByAircraftIdAndSeatNumber() {
        Aircraft aircraft = createAircraft();
        Seat seat = createSeat(aircraft, "12F", "BUSINESS");
        seatRepository.save(seat);

        Optional<Seat> found = seatRepository.findByAircraftIdAndSeatNumber(aircraft.getId(), "12F");
        assertThat(found).isPresent();
        assertThat(found.get().getSeatClass()).isEqualTo("BUSINESS");
    }

    @Test
    void findAvailableSeatsForFlight() {
        Aircraft aircraft = createAircraft();
        Flight flight = createFlight(aircraft);
        
        Seat seat1 = createSeat(aircraft, "1A", "ECONOMY");
        Seat seat2 = createSeat(aircraft, "1B", "ECONOMY");
        Seat seat3 = createSeat(aircraft, "1C", "ECONOMY");
        seatRepository.save(seat1);
        seatRepository.save(seat2);
        seatRepository.save(seat3);

        // Allocate seat1 for the flight
        Ticket ticket = createTicket();
        SeatAllocation allocation = new SeatAllocation();
        allocation.setSeat(seat1);
        allocation.setTicket(ticket);
        allocation.setFlight(flight);
        seatAllocationRepository.save(allocation);

        List<Seat> available = seatRepository.findAvailableSeatsForFlight(aircraft.getId(), flight.getId());
        assertThat(available).hasSize(2);
        assertThat(available).noneMatch(s -> s.getSeatNumber().equals("1A"));
    }

    @Test
    void isSeatAllocatedForFlight() {
        Aircraft aircraft = createAircraft();
        Flight flight = createFlight(aircraft);
        
        Seat seat = createSeat(aircraft, "1A", "ECONOMY");
        seatRepository.save(seat);

        Ticket ticket = createTicket();
        SeatAllocation allocation = new SeatAllocation();
        allocation.setSeat(seat);
        allocation.setTicket(ticket);
        allocation.setFlight(flight);
        seatAllocationRepository.save(allocation);

        boolean isAllocated = seatRepository.isSeatAllocatedForFlight(seat.getId(), flight.getId());
        assertThat(isAllocated).isTrue();
    }

    @Test
    void isSeatAllocatedForFlight_notAllocated() {
        Aircraft aircraft = createAircraft();
        Flight flight = createFlight(aircraft);
        
        Seat seat = createSeat(aircraft, "1A", "ECONOMY");
        seatRepository.save(seat);

        boolean isAllocated = seatRepository.isSeatAllocatedForFlight(seat.getId(), flight.getId());
        assertThat(isAllocated).isFalse();
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

    private Seat createSeat(Aircraft aircraft, String seatNumber, String seatClass) {
        Seat seat = new Seat();
        seat.setAircraft(aircraft);
        seat.setSeatNumber(seatNumber);
        seat.setSeatClass(seatClass);
        seat.setRowNumber(Short.parseShort(seatNumber.replaceAll("[A-Z]", "")));
        seat.setColumnLetter(seatNumber.replaceAll("[0-9]", ""));
        seat.setIsActive(true);
        return seat;
    }

    private Ticket createTicket() {
        Ticket ticket = new Ticket();
        ticket.setTicketNumber("TKT" + System.currentTimeMillis());
        ticket.setFare(java.math.BigDecimal.valueOf(100.00));
        ticket.setTaxes(java.math.BigDecimal.valueOf(20.00));
        return ticketRepository.save(ticket);
    }
}
