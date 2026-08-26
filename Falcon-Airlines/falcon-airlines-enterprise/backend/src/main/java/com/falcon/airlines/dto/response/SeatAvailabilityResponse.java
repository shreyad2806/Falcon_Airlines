package com.falcon.airlines.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SeatAvailabilityResponse {

    private Long flightId;
    private String flightNumber;
    private Long aircraftId;
    private String aircraftRegistrationNumber;
    private String aircraftType;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer bookedSeats;
    private Integer blockedSeats;
    private List<SeatDetailResponse> seats;

    @Getter
    @Setter
    public static class SeatDetailResponse {

        private Long seatId;
        private String seatNumber;
        private String seatClass;
        private String seatType;
        private Short rowNumber;
        private String columnLetter;
        private Boolean isAvailable;
        private Boolean isActive;
        private BigDecimal price;
        private String status; // AVAILABLE, OCCUPIED, BLOCKED
    }
}
