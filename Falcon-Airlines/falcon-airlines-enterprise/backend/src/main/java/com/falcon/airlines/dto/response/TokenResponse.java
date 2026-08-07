package com.falcon.airlines.dto.response;

import com.falcon.airlines.enums.TokenStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Authentication token payload returned after a successful login or refresh.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private Instant issuedAt;
    private Instant expiresAt;
    private TokenStatus refreshTokenStatus;
    private String username;
    private Long userId;
}
