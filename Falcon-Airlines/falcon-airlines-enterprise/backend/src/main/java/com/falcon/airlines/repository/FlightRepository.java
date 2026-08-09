package com.falcon.airlines.repository;

import com.falcon.airlines.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Long>, JpaSpecificationExecutor<Flight> {

    Optional<Flight> findByFlightNumber(String flightNumber);

    List<Flight> findByOriginAirportIdAndDestinationAirportId(Long originId, Long destinationId);

    boolean existsByOriginAirportIdAndIsActiveTrue(Long originAirportId);

    boolean existsByDestinationAirportIdAndIsActiveTrue(Long destinationAirportId);

    boolean existsByAircraftIdAndIsActiveTrue(Long aircraftId);

    Optional<Flight> findByFlightNumberAndScheduledDeparture(String flightNumber, Instant scheduledDeparture);

    Optional<Flight> findByFlightNumberAndScheduledDepartureAndIdNot(String flightNumber, Instant scheduledDeparture, Long id);

    List<Flight> findByAircraftIdAndScheduledArrivalGreaterThanAndScheduledDepartureLessThanAndIsActiveTrue(
            Long aircraftId, Instant newDeparture, Instant newArrival);

    List<Flight> findByAircraftIdAndScheduledArrivalGreaterThanAndScheduledDepartureLessThanAndIsActiveTrueAndIdNot(
            Long aircraftId, Instant newDeparture, Instant newArrival, Long id);
}
