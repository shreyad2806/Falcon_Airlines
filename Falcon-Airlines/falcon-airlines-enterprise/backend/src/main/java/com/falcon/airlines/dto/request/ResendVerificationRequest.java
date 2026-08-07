package com.falcon.airlines.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request to resend/generate an email verification token.
 */
@Getter
@Setter
public class ResendVerificationRequest {

    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;
}
