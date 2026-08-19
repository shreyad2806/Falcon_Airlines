package com.falcon.airlines.dto.response;

import com.falcon.airlines.enums.TicketStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class TicketDetailResponse {

    private Long id;
    private String ticketNumber;
    
    // Booking Information
    private Long bookingId;
    private String bookingReference;
    
    // Passenger Information
    private Long passengerId;
    private String passengerName;
    private String passengerFirstName;
    private String passengerLastName;
    
    // Flight Information
    private Long flightId;
    private String flightNumber;
    private String airline;
    
    // Route Information
    private Long originAirportId;
    private String originAirportCode;
    private String originAirportName;
    private String originCity;
    
    private Long destinationAirportId;
    private String destinationAirportCode;
    private String destinationAirportName;
    private String destinationCity;
    
    // Schedule Information
    private Instant scheduledDeparture;
    private Instant scheduledArrival;
    
    // Seat Information
    private Long seatId;
    private String seatNumber;
    private String seatClass;
    
    // Fare Information
    private String fareBasis;
    private BigDecimal fare;
    private BigDecimal taxes;
    private BigDecimal totalAmount;
    private String currency;
    
    // Boarding Information
    private String terminal;
    private String gate;
    private Instant boardingTime;
    
    // Ticket Status
    private TicketStatus status;
    private Instant issuedAt;
    
    // Audit Information
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
