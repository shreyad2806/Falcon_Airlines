package com.falcon.airlines.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookingRequest {

    @NotNull
    private Long customerId;

    @NotNull
    private Long flightId;

    @NotEmpty(message = "At least one passenger must be provided")
    @Valid
    private List<BookingPassengerRequest> passengers;

    @NotEmpty(message = "At least one seat must be requested")
    private List<String> requestedSeats;

    @Getter
    @Setter
    public static class BookingPassengerRequest {

        @NotNull(message = "Passenger ID is required")
        private Long passengerId;

        private String fareClass;

        private String cabin;
    }
}
