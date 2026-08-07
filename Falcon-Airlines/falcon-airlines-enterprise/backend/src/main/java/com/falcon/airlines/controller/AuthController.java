package com.falcon.airlines.controller;

import com.falcon.airlines.dto.request.LoginRequest;
import com.falcon.airlines.dto.request.LogoutRequest;
import com.falcon.airlines.dto.request.RefreshTokenRequest;
import com.falcon.airlines.dto.request.RegisterRequest;
import com.falcon.airlines.dto.response.TokenResponse;
import com.falcon.airlines.dto.response.UserResponse;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints for the Falcon Airlines platform.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new customer account.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse userResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered successfully", userResponse));
    }

    /**
     * Authenticates a user and returns JWT access and refresh tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", tokenResponse));
    }

    /**
     * Exchanges a valid refresh token for a new JWT access token and a rotated refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", tokenResponse));
    }

    /**
     * Invalidates the supplied refresh token and deletes its database record.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
