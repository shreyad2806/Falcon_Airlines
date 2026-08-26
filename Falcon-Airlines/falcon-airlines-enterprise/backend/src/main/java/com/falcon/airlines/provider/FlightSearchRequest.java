package com.falcon.airlines.provider;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Flight search parameters used across all providers.
 */
@Data
@Builder
public class FlightSearchRequest {

    private String origin;          // IATA code
    private String destination;     // IATA code
    private LocalDate departureDate;
    private String flightNumber;
    private String status;
    private int page;
    private int size;
}
