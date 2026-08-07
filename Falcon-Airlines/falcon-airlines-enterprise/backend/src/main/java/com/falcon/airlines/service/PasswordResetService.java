package com.falcon.airlines.service;

import com.falcon.airlines.dto.request.PasswordResetConfirmRequest;
import com.falcon.airlines.dto.request.PasswordResetRequest;
import com.falcon.airlines.entity.PasswordResetToken;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.enums.UserStatus;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.repository.PasswordResetTokenRepository;
import com.falcon.airlines.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Manages self-service password reset token lifecycle.
 * <p>
 * No email is sent; the generated token is returned directly so the caller
 * (or a future email integration) can deliver it.
 */
@Service
@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final long expirationSeconds;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                BCryptPasswordEncoder passwordEncoder,
                                @Value("${password-reset.expiration-seconds:3600}") long expirationSeconds) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.expirationSeconds = expirationSeconds;
    }

    /**
     * Generates a one-time password reset token for the supplied username or email.
     */
    public String requestReset(PasswordResetRequest request) {
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new BaseException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BaseException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
        }

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(expirationSeconds));
        passwordResetTokenRepository.save(token);

        return token.getToken();
    }

    /**
     * Validates the token and updates the user's password.
     */
    public void confirmReset(PasswordResetConfirmRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BaseException("Invalid or expired token", HttpStatus.BAD_REQUEST, "INVALID_TOKEN"));

        if (token.getExpiresAt().isBefore(Instant.now())
                || token.isDeleted()
                || token.getUser() == null
                || token.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new BaseException("Invalid or expired token", HttpStatus.BAD_REQUEST, "INVALID_TOKEN");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.delete(token);
    }
}
