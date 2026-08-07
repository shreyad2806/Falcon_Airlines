package com.falcon.airlines.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Utility helpers for resolving JWTs from incoming HTTP requests.
 */
@Component
public class JwtTokenUtil {

    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * Extracts the raw JWT string from the {@code Authorization} header if it is a Bearer token.
     */
    public Optional<String> resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()).trim());
        }
        return Optional.empty();
    }

    /**
     * Convenience check for whether the request carries an Authorization Bearer token.
     */
    public boolean hasBearerToken(HttpServletRequest request) {
        return resolveToken(request).isPresent();
    }
}
