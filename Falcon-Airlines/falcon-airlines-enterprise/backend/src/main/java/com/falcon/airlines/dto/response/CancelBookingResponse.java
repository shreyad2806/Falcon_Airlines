package com.falcon.airlines.dto.response;

import com.falcon.airlines.enums.BookingStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class CancelBookingResponse {

    private Long bookingId;
    private String bookingReference;
    private BookingStatus previousStatus;
    private BookingStatus newStatus;
    private BigDecimal refundAmount;
    private String currency;
    private Instant cancelledAt;
    private String cancellationReason;
}
