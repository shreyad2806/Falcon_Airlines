package com.falcon.airlines.repository;

import com.falcon.airlines.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Long>, JpaSpecificationExecutor<Flight> {

    Optional<Flight> findByFlightNumber(String flightNumber);

    List<Flight> findByOriginAirportIdAndDestinationAirportId(Long originId, Long destinationId);
}
