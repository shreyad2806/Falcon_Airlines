package com.falcon.airlines.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request to validate an email verification token.
 */
@Getter
@Setter
public class VerifyEmailRequest {

    @NotBlank(message = "Verification token is required")
    private String token;
}
