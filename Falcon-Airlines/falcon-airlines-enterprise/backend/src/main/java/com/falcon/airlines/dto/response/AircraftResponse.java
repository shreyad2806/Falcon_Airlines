package com.falcon.airlines.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class AircraftResponse {

    private Long id;
    private String registrationNumber;
    private String type;
    private String model;
    private String manufacturer;
    private Short totalCapacity;
    private String configuration;
    private Instant createdAt;
    private Instant updatedAt;
}
