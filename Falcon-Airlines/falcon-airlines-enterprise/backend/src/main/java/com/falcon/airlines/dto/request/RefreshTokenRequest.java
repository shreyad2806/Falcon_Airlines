package com.falcon.airlines.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for exchanging a refresh token for a new access token.
 */
@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank
    @Size(max = 512)
    private String refreshToken;

    @Size(max = 255)
    private String deviceInfo;

    @Size(max = 45)
    private String ipAddress;

    @Size(max = 512)
    private String userAgent;
}
