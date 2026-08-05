package com.falcon.airlines.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AirportRequest {

    @NotBlank
    @Size(min = 3, max = 3)
    private String iataCode;

    @Size(min = 4, max = 4)
    private String icaoCode;

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(min = 2, max = 2)
    private String country;

    @NotBlank
    @Size(max = 50)
    private String timeZone;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isActive;
}
