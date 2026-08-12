package com.falcon.airlines.dto.response;

import com.falcon.airlines.enums.TicketStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class TicketSummaryResponse {

    private Long id;
    private String ticketNumber;
    private Long passengerId;
    private String passengerName;
    private Long flightId;
    private String flightNumber;
    private String seatNumber;
    private String seatClass;
    private String fareBasis;
    private BigDecimal fare;
    private BigDecimal taxes;
    private TicketStatus status;
    private Instant issuedAt;
}
