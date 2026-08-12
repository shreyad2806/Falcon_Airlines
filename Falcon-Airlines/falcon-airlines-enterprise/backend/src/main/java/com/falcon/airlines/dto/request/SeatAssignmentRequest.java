package com.falcon.airlines.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatAssignmentRequest {

    @NotNull(message = "Ticket ID is required")
    private Long ticketId;

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    @NotBlank(message = "Seat number is required")
    private String seatNumber;
}
