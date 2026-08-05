package com.falcon.airlines.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AircraftRequest {

    @NotBlank
    @Size(max = 20)
    private String registrationNumber;

    @NotBlank
    @Size(max = 50)
    private String type;

    @NotBlank
    @Size(max = 100)
    private String model;

    @NotBlank
    @Size(max = 100)
    private String manufacturer;

    @NotNull
    @Min(1)
    private Short totalCapacity;

    private String configuration;
}
