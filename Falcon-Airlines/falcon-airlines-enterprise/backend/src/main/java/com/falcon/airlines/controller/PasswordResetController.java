package com.falcon.airlines.controller;

import com.falcon.airlines.dto.request.PasswordResetConfirmRequest;
import com.falcon.airlines.dto.request.PasswordResetRequest;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public password reset endpoints.
 * <p>
 * No email integration is implemented; the generated reset token is returned
 * directly by {@code /auth/forgot-password} for testing/manual delivery.
 */
@RestController
@RequestMapping("/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        String token = passwordResetService.requestReset(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset token generated", token));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request);
        return ResponseEntity.ok(ApiResponse.ok("Password has been reset successfully"));
    }
}
