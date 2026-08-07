package com.falcon.airlines.controller;

import com.falcon.airlines.dto.request.ResendVerificationRequest;
import com.falcon.airlines.dto.request.VerifyEmailRequest;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public email verification endpoints.
 * <p>
 * No SMTP integration is implemented; the generated token is returned directly
 * by {@code /auth/resend-verification} for testing/manual delivery.
 */
@RestController
@RequestMapping("/auth")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<String>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        String token = emailVerificationService.resendVerification(request);
        return ResponseEntity.ok(ApiResponse.ok("Verification token generated", token));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.ok("Email verified successfully"));
    }
}
