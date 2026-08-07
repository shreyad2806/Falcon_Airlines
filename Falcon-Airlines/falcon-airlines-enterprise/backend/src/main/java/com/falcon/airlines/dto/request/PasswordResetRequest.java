package com.falcon.airlines.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request to initiate a password reset.
 */
@Getter
@Setter
public class PasswordResetRequest {

    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;
}
