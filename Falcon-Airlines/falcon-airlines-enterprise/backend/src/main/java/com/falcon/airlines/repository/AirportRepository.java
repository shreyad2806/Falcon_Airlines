package com.falcon.airlines.repository;

import com.falcon.airlines.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AirportRepository extends JpaRepository<Airport, Long>, JpaSpecificationExecutor<Airport> {

    Optional<Airport> findByIataCode(String iataCode);

    Optional<Airport> findByIcaoCode(String icaoCode);

    Optional<Airport> findByIataCodeAndIdNot(String iataCode, Long id);

    Optional<Airport> findByIcaoCodeAndIdNot(String icaoCode, Long id);
}
