package com.falcon.airlines.dto.response;

import com.falcon.airlines.enums.FlightStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class FlightResponse {

    private Long id;
    private String flightNumber;
    private Long originAirportId;
    private String originAirportIataCode;
    private String originAirportName;
    private Long destinationAirportId;
    private String destinationAirportIataCode;
    private String destinationAirportName;
    private Long aircraftId;
    private String aircraftRegistrationNumber;
    private Instant scheduledDeparture;
    private Instant scheduledArrival;
    private FlightStatus status;
    private String terminal;
    private String gate;
    private Boolean isActive;
    private BigDecimal basePrice;
    private String currency;
    private Integer availableSeats;
    private Instant createdAt;
    private Instant updatedAt;
}
