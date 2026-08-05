package com.falcon.airlines.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class AirportResponse {

    private Long id;
    private String iataCode;
    private String icaoCode;
    private String name;
    private String city;
    private String country;
    private String timeZone;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
