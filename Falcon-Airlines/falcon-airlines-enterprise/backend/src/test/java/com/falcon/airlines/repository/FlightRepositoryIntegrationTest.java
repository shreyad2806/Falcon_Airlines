package com.falcon.airlines.repository;

import com.falcon.airlines.common.BaseIntegrationTest;
import com.falcon.airlines.entity.Aircraft;
import com.falcon.airlines.entity.Airport;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.enums.FlightStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class FlightRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Test
    void findByIdReturnsFlight() {
        Optional<Flight> found = flightRepository.findById(1L);
        assertThat(found).isPresent();
        assertThat(found.get().getFlightNumber()).isEqualTo("FA101");
    }

    @Test
    void saveAndFindFlight() {
        Airport origin = airportRepository.findById(1L).orElseThrow();
        Airport dest = airportRepository.findById(2L).orElseThrow();
        Aircraft aircraft = aircraftRepository.findById(1L).orElseThrow();

        Flight flight = new Flight();
        flight.setFlightNumber("FA999");
        flight.setOriginAirport(origin);
        flight.setDestinationAirport(dest);
        flight.setAircraft(aircraft);
        flight.setScheduledDeparture(Instant.parse("2026-12-01T10:00:00Z"));
        flight.setScheduledArrival(Instant.parse("2026-12-01T14:00:00Z"));
        flight.setStatus(FlightStatus.SCHEDULED);
        flight.setIsActive(true);

        Flight saved = flightRepository.save(flight);
        assertThat(saved.getId()).isNotNull();

        Optional<Flight> loaded = flightRepository.findById(saved.getId());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getFlightNumber()).isEqualTo("FA999");
    }

    @Test
    void findByFlightNumberAndScheduledDepartureDetectsDuplicate() {
        Flight existing = flightRepository.findById(1L).orElseThrow();
        Optional<Flight> duplicate = flightRepository.findByFlightNumberAndScheduledDeparture(
                existing.getFlightNumber(), existing.getScheduledDeparture());
        assertThat(duplicate).isPresent();
    }

    @Test
    void aircraftOverlapDetection() {
        // Existing FA101 uses aircraft 1 from 2026-08-15 08:00 to 20:00 UTC
        List<Flight> overlapping = flightRepository
                .findByAircraftIdAndScheduledArrivalGreaterThanAndScheduledDepartureLessThanAndIsActiveTrue(
                        1L, Instant.parse("2026-08-15T09:00:00Z"), Instant.parse("2026-08-15T21:00:00Z"));
        assertThat(overlapping).isNotEmpty();
    }

    @Test
    void specificationBasedFilteringAndPagination() {
        Specification<Flight> spec = (root, query, cb) ->
                cb.equal(root.get("status"), FlightStatus.SCHEDULED);

        Page<Flight> page = flightRepository.findAll(spec,
                PageRequest.of(0, 2, Sort.by("scheduledDeparture").ascending()));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent()).hasSizeLessThanOrEqualTo(2);
    }
}
