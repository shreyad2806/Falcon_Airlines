package com.falcon.airlines.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for username/email and password login.
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank
    @Size(max = 255)
    private String usernameOrEmail;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    @Size(max = 255)
    private String deviceInfo;

    @Size(max = 512)
    private String userAgent;

    @Size(max = 45)
    private String ipAddress;
}
