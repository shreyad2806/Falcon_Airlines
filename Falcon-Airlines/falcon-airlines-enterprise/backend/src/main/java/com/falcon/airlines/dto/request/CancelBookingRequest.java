package com.falcon.airlines.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelBookingRequest {

    @NotNull(message = "Cancellation reason is required")
    private String cancellationReason;
}
