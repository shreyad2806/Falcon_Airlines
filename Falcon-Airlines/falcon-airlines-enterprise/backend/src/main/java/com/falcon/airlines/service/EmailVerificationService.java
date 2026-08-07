package com.falcon.airlines.service;

import com.falcon.airlines.dto.request.ResendVerificationRequest;
import com.falcon.airlines.dto.request.VerifyEmailRequest;
import com.falcon.airlines.entity.EmailVerificationToken;
import com.falcon.airlines.entity.User;
import com.falcon.airlines.enums.UserStatus;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.repository.EmailVerificationTokenRepository;
import com.falcon.airlines.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Manages email verification token lifecycle.
 * <p>
 * No SMTP is implemented; the generated token is returned directly so a future
 * email integration can deliver it.
 */
@Service
@Transactional
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final long expirationSeconds;

    public EmailVerificationService(UserRepository userRepository,
                                    EmailVerificationTokenRepository emailVerificationTokenRepository,
                                    @Value("${email-verification.expiration-seconds:86400}") long expirationSeconds) {
        this.userRepository = userRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.expirationSeconds = expirationSeconds;
    }

    /**
     * Generates a verification token for the supplied username or email.
     */
    public String resendVerification(ResendVerificationRequest request) {
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new BaseException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BaseException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
        }

        List<EmailVerificationToken> existing = emailVerificationTokenRepository.findByUser(user);
        if (!existing.isEmpty()) {
            emailVerificationTokenRepository.deleteAll(existing);
        }

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(expirationSeconds));
        emailVerificationTokenRepository.save(token);

        return token.getToken();
    }

    /**
     * Validates the token and marks the user's email as verified.
     */
    public void verifyEmail(VerifyEmailRequest request) {
        EmailVerificationToken token = emailVerificationTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BaseException("Invalid or expired token", HttpStatus.BAD_REQUEST, "INVALID_TOKEN"));

        if (token.getExpiresAt().isBefore(Instant.now())
                || token.isDeleted()
                || token.getUser() == null
                || token.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new BaseException("Invalid or expired token", HttpStatus.BAD_REQUEST, "INVALID_TOKEN");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        emailVerificationTokenRepository.delete(token);
    }
}
