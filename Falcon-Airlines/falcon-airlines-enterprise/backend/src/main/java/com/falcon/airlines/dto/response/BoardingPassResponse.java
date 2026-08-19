package com.falcon.airlines.dto.response;

import com.falcon.airlines.enums.BoardingPassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardingPassResponse {
    private Long id;
    private String boardingPassNumber;
    private Long ticketId;
    private String ticketNumber;
    private Long passengerId;
    private String passengerName;
    private String passengerFirstName;
    private String passengerLastName;
    private Long flightId;
    private String flightNumber;
    private Long bookingId;
    private String bookingReference;
    private String seatNumber;
    private String seatClass;
    private String boardingGroup;
    private String gate;
    private Instant boardingTime;
    private BoardingPassStatus status;
    private String qrCodePayload;
    private Instant generatedAt;
    private Instant checkedInAt;
    private Instant boardedAt;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Flight information for display
    private String originAirportCode;
    private String originAirportName;
    private String originCity;
    private String destinationAirportCode;
    private String destinationAirportName;
    private String destinationCity;
    private Instant scheduledDeparture;
    private Instant scheduledArrival;
    private String terminal;
    private String airline;
}
