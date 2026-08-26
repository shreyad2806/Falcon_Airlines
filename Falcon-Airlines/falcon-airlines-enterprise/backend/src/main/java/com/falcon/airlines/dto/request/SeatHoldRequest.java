package com.falcon.airlines.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatHoldRequest {

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    @NotNull(message = "Seat ID is required")
    private Long seatId;
}
