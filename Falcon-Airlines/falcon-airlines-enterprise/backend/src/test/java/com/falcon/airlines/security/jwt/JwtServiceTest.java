package com.falcon.airlines.security.jwt;

import com.falcon.airlines.security.principal.UserPrincipal;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JWT token generation, parsing, and validation.
 */
class JwtServiceTest {

    private static final String SECRET = "this-is-a-very-strong-test-secret-key-32";
    private static final long ACCESS_EXPIRATION = 900;
    private static final long REFRESH_EXPIRATION = 604800;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION));
    }

    @Test
    void shouldGenerateAndExtractUsername() {
        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        when(userPrincipal.getUsername()).thenReturn("jane_doe");
        when(userPrincipal.getId()).thenReturn(42L);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))).when(userPrincipal).getAuthorities();

        String token = jwtService.generateAccessToken(userPrincipal);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("jane_doe");
    }

    @Test
    void shouldValidateTokenForMatchingUser() {
        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        when(userPrincipal.getUsername()).thenReturn("john_doe");
        when(userPrincipal.getId()).thenReturn(1L);
        doReturn(List.of()).when(userPrincipal).getAuthorities();

        String token = jwtService.generateAccessToken(userPrincipal);

        UserDetails userDetails = new User("john_doe", "", List.of());
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void shouldRejectTokenForDifferentUser() {
        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        when(userPrincipal.getUsername()).thenReturn("john_doe");
        when(userPrincipal.getId()).thenReturn(1L);
        doReturn(List.of()).when(userPrincipal).getAuthorities();

        String token = jwtService.generateAccessToken(userPrincipal);

        UserDetails otherUser = new User("jane_doe", "", List.of());
        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("john_doe")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(key)
                .compact();

        UserDetails userDetails = new User("john_doe", "", List.of());
        assertThat(jwtService.isTokenValid(expiredToken, userDetails)).isFalse();
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() {
        SecretKey otherKey = Keys.hmacShaKeyFor("a-different-secret-key-32-bytes-long".getBytes(StandardCharsets.UTF_8));
        String forgedToken = Jwts.builder()
                .subject("john_doe")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(ACCESS_EXPIRATION)))
                .signWith(otherKey)
                .compact();

        UserDetails userDetails = new User("john_doe", "", List.of());
        assertThat(jwtService.isTokenValid(forgedToken, userDetails)).isFalse();
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThatThrownBy(() -> jwtService.extractUsername("not-a-jwt"))
                .isInstanceOfAny(io.jsonwebtoken.JwtException.class, IllegalArgumentException.class);
    }

    @Test
    void shouldExtractAuthoritiesClaim() {
        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        when(userPrincipal.getUsername()).thenReturn("admin");
        when(userPrincipal.getId()).thenReturn(99L);
        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("BOOKING_WRITE"))).when(userPrincipal).getAuthorities();

        String token = jwtService.generateAccessToken(userPrincipal);

        List<String> roles = jwtService.extractClaim(token, claims -> claims.get("roles", List.class));
        assertThat(roles).containsExactly("ROLE_ADMIN", "BOOKING_WRITE");
    }
}
