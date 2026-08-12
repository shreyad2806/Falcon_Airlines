package com.falcon.airlines.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SeatAvailabilityResponse {

    private Long flightId;
    private String flightNumber;
    private Long aircraftId;
    private String aircraftRegistrationNumber;
    private Integer totalSeats;
    private Integer availableSeats;
    private List<SeatDetailResponse> seats;

    @Getter
    @Setter
    public static class SeatDetailResponse {

        private Long seatId;
        private String seatNumber;
        private String seatClass;
        private Short rowNumber;
        private String columnLetter;
        private Boolean isAvailable;
        private Boolean isActive;
    }
}
