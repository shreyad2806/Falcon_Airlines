package com.falcon.airlines.dto.request;

import com.falcon.airlines.enums.BookingPaymentStatus;
import com.falcon.airlines.enums.BookingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class BookingRequest {

    @NotBlank
    @Size(min = 6, max = 10)
    private String bookingReference;

    @NotNull
    private Long customerId;

    @NotNull
    private BookingStatus status;

    @NotNull
    @PositiveOrZero
    private BigDecimal totalAmount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    private BookingPaymentStatus paymentStatus;

    private Instant timeLimit;
}
