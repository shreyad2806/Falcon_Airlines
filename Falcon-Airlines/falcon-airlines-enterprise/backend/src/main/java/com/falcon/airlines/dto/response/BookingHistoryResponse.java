package com.falcon.airlines.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookingHistoryResponse {

    private Long customerId;
    private String customerUsername;
    private Integer totalBookings;
    private List<BookingSummaryResponse> bookings;
}
