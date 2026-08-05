package com.falcon.airlines.dto.response;

import com.falcon.airlines.enums.BookingPaymentStatus;
import com.falcon.airlines.enums.BookingStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class BookingResponse {

    private Long id;
    private String bookingReference;
    private Long customerId;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private Instant bookingDate;
    private Instant timeLimit;
    private BookingPaymentStatus paymentStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
