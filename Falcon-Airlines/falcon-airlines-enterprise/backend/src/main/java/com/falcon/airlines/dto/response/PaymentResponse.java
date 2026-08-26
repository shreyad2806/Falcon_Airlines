package com.falcon.airlines.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class PaymentResponse {

    private Long paymentId;
    private Long bookingId;
    private String bookingReference;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String status;
    private String gatewayReference;
    private Instant paidAt;
    private String bookingStatus;
}
