package com.falcon.airlines.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Stateless JWT authentication filter that validates Bearer tokens on each request.
 * <p>
 * For valid tokens, the user is re-loaded via {@link UserDetailsService} and an
 * authentication object is placed into the {@link SecurityContextHolder}.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil,
                                   JwtService jwtService,
                                   UserDetailsService userDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Optional<String> token = jwtTokenUtil.resolveToken(request);

        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = token.get();
        try {
            String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    // Token is present but invalid/expired — return 401 so the
                    // client interceptor can attempt a refresh before the request
                    // silently falls through to Spring Security's 403.
                    log.warn("Expired/invalid JWT for path {}", request.getRequestURI());
                    writeUnauthorized(response, "Token expired or invalid");
                    return;
                }
            }
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token for path {}: {}", request.getRequestURI(), ex.getMessage());
            writeUnauthorized(response, "Invalid token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String body = "{\"success\":false,\"status\":401,\"error\":\"AUTHENTICATION_ERROR\",\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }
}
