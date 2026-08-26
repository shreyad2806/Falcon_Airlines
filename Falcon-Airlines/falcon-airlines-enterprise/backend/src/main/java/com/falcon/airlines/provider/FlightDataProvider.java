package com.falcon.airlines.provider;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Abstraction for flight data sources.
 * Implementations can provide flights from the internal database
 * or from external flight APIs (Aviationstack, Amadeus, etc.).
 *
 * Switch providers via: flight.provider=mock  or  flight.provider=external
 */
public interface FlightDataProvider {

    /**
     * Search flights matching the given criteria.
     * Returns normalized flight data regardless of source.
     */
    List<NormalizedFlight> searchFlights(FlightSearchRequest request);

    /**
     * Get live status for a specific flight on a given date.
     */
    Optional<NormalizedFlight> getFlightStatus(String flightNumber, LocalDate date);

    /**
     * Check if this provider is healthy and available.
     */
    boolean isAvailable();

    /**
     * Get the provider name for logging/display.
     */
    String getProviderName();
}
