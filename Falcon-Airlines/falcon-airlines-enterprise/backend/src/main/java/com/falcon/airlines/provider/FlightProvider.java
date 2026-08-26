package com.falcon.airlines.provider;

import com.falcon.airlines.entity.Flight;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction for flight data sources.
 * Implementations can provide flights from the internal database
 * or from external flight providers (GDS, airline APIs, etc.).
 */
public interface FlightProvider {

    /**
     * Search for available flights matching the given criteria.
     */
    List<Flight> searchFlights(String origin, String destination, String date);

    /**
     * Get a specific flight by ID.
     */
    Optional<Flight> getFlight(Long id);

    /**
     * Get the base price for a flight in the specified currency.
     * Returns the price in INR by default.
     */
    java.math.BigDecimal getBasePrice(Long flightId, String currency);

    /**
     * Get seat pricing for a specific seat category.
     * Returns the additional fee on top of the base fare.
     */
    java.math.BigDecimal getSeatFee(String seatCategory, String currency);

    /**
     * Check if the provider is available (for health checks).
     */
    boolean isAvailable();
}
