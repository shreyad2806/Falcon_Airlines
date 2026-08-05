package com.falcon.airlines.dto.request;

import com.falcon.airlines.enums.FlightStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class FlightRequest {

    @NotBlank
    @Size(max = 10)
    private String flightNumber;

    @NotNull
    private Long originAirportId;

    @NotNull
    private Long destinationAirportId;

    @NotNull
    private Long aircraftId;

    @NotNull
    private Instant scheduledDeparture;

    @NotNull
    private Instant scheduledArrival;

    @NotNull
    private FlightStatus status;

    @Size(max = 10)
    private String terminal;

    @Size(max = 10)
    private String gate;

    private Boolean isActive;
}
