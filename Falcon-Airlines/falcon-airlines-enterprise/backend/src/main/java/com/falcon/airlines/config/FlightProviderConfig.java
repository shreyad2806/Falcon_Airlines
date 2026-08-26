package com.falcon.airlines.config;

import com.falcon.airlines.provider.DatabaseFlightDataProvider;
import com.falcon.airlines.provider.FlightDataProvider;
import com.falcon.airlines.repository.FlightRepository;
import com.falcon.airlines.repository.SeatAllocationRepository;
import com.falcon.airlines.repository.SeatRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the database flight provider bean.
 * This is always available as a fallback when the primary provider fails.
 */
@Configuration
public class FlightProviderConfig {

    @Bean("databaseFlightProvider")
    public FlightDataProvider databaseFlightProvider(
            FlightRepository flightRepository,
            SeatRepository seatRepository,
            SeatAllocationRepository seatAllocationRepository) {
        return new DatabaseFlightDataProvider(flightRepository, seatRepository, seatAllocationRepository);
    }
}
