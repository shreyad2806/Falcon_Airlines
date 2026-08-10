package com.falcon.airlines.dto.response;

import com.falcon.airlines.enums.Gender;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class PassengerResponse {

    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String email;
    private String phone;
    private String passportNumber;
    private String nationality;
    private Gender gender;
    private String redressNumber;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer ticketCount;
    private Integer bookingCount;
}
