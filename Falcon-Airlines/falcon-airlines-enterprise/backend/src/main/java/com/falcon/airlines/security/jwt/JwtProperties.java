package com.falcon.airlines.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * Type-safe externalised configuration for JWT signing and token lifetimes.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes long");
        }
        if (accessTokenExpiration <= 0) {
            accessTokenExpiration = 900;
        }
        if (refreshTokenExpiration <= 0) {
            refreshTokenExpiration = 604800;
        }
    }
}
