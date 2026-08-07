package com.falcon.airlines.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for registering a new passenger or staff user.
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username may contain only letters, digits, underscores, dots, and hyphens")
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "Password must contain upper and lower case letters and a digit")
    private String password;

    @Size(max = 20)
    private String mobileNumber;

    @Size(max = 255)
    private String deviceInfo;

    @Size(max = 512)
    private String userAgent;

    @Size(max = 45)
    private String ipAddress;
}
