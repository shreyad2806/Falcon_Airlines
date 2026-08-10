package com.falcon.airlines.dto.request;

import com.falcon.airlines.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,20}$", message = "Phone number must be valid")
    @Size(max = 20)
    private String phone;

    @Pattern(regexp = "^[A-Z0-9<]{6,50}$", message = "Passport number must be valid (alphanumeric)")
    @Size(max = 50)
    private String passportNumber;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Nationality must be a valid 3-letter ISO country code")
    @Size(max = 3)
    private String nationality;

    @NotNull
    private Gender gender;

    @Size(max = 20)
    private String redressNumber;
}
