package com.falcon.airlines.dto.request;

import com.falcon.airlines.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PassengerRequest {

    private Long userId;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 50)
    private String passportNumber;

    @Size(max = 3)
    private String nationality;

    @NotNull
    private Gender gender;

    @Size(max = 20)
    private String redressNumber;
}
