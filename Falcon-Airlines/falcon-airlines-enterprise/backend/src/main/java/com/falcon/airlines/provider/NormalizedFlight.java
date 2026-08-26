package com.falcon.airlines.provider;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Provider-agnostic flight data model.
 * All external API responses are mapped into this before reaching the frontend.
 */
@Data
@Builder
public class NormalizedFlight {

    private Long internalId;          // Falcon DB id (null for external-only flights)
    private String flightNumber;
    private String airlineName;
    private String airlineCode;

    // Route
    private String originCode;        // IATA
    private String originName;
    private String originCity;
    private String destinationCode;   // IATA
    private String destinationName;
    private String destinationCity;

    // Times (UTC)
    private Instant scheduledDeparture;
    private Instant scheduledArrival;
    private Instant estimatedDeparture;
    private Instant estimatedArrival;
    private Instant actualDeparture;
    private Instant actualArrival;

    // Status
    private String status;            // SCHEDULED, DELAYED, CANCELLED, BOARDING, DEPARTED, ARRIVED, LANDED
    private String statusDetail;      // Human-readable status note

    // Duration
    private Duration duration;

    // Aircraft
    private String aircraftType;
    private String aircraftRegistration;

    // Inventory (Falcon-managed)
    private boolean inventoryManagedByFalcon;
    private int totalSeats;
    private int availableSeats;

    // Pricing (Falcon-managed)
    private BigDecimal basePrice;
    private String currency;

    // Booking eligibility
    private boolean bookable;
    private String bookingUnavailableReason;

    // Terminal/gate
    private String terminal;
    private String gate;

    // Data source tracking
    private String dataSource;            // LIVE, DATABASE_FALLBACK, MOCK
    private Instant lastUpdated;          // When this data was last refreshed
}
