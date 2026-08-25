package com.falcon.airlines.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

/**
 * Utility for generating and verifying QR code verification tokens.
 * Uses JWT for secure, tamper-proof tokens that can be validated by the backend.
 */
@Component
public class QrTokenUtil {

    @Value("${falcon.airlines.qr.secret:falcon-qr-secret-key-change-in-production}")
    private String qrSecret;

    @Value("${falcon.airlines.qr.token-validity-hours:24}")
    private long tokenValidityHours;

    // Setter for testing
    public void setQrSecret(String qrSecret) {
        this.qrSecret = qrSecret;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = qrSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a secure verification token for a boarding pass.
     * The token contains minimal information (boarding pass ID and status) for verification.
     */
    public String generateVerificationToken(Long boardingPassId, String boardingPassNumber, String status) {
        Instant now = Instant.now();
        Instant expiration = now.plus(tokenValidityHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(boardingPassId != null ? boardingPassId.toString() : "0")
                .claim("bpn", boardingPassNumber)
                .claim("status", status)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Verify a QR code verification token.
     * Returns the boarding pass ID if valid, null otherwise.
     */
    public Long verifyToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            return subject != null ? Long.parseLong(subject) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract boarding pass number from token.
     */
    public String extractBoardingPassNumber(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("bpn", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract status from token.
     */
    public String extractStatus(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get("status", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
