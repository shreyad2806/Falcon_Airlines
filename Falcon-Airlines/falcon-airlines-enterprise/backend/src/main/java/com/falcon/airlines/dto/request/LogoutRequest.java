package com.falcon.airlines.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for invalidating a refresh token during logout.
 */
@Getter
@Setter
public class LogoutRequest {

    @NotBlank
    @Size(max = 512)
    private String refreshToken;
}
