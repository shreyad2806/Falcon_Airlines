package com.falcon.airlines.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrTokenUtilTest {

    private QrTokenUtil qrTokenUtil;

    @BeforeEach
    void setUp() {
        qrTokenUtil = new QrTokenUtil();
        // Set a test secret
        qrTokenUtil.setQrSecret("test-secret-key-for-testing");
    }

    @Test
    void generateVerificationToken_success() {
        Long boardingPassId = 123L;
        String boardingPassNumber = "BP123456";
        String status = "GENERATED";

        String token = qrTokenUtil.generateVerificationToken(boardingPassId, boardingPassNumber, status);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        // JWT tokens have 3 parts separated by dots
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void verifyToken_success() {
        Long boardingPassId = 123L;
        String boardingPassNumber = "BP123456";
        String status = "GENERATED";

        String token = qrTokenUtil.generateVerificationToken(boardingPassId, boardingPassNumber, status);
        Long verifiedId = qrTokenUtil.verifyToken(token);

        assertThat(verifiedId).isEqualTo(boardingPassId);
    }

    @Test
    void verifyToken_invalid() {
        String invalidToken = "invalid.token.here";

        Long verifiedId = qrTokenUtil.verifyToken(invalidToken);

        assertThat(verifiedId).isNull();
    }

    @Test
    void extractBoardingPassNumber_success() {
        Long boardingPassId = 123L;
        String boardingPassNumber = "BP123456";
        String status = "GENERATED";

        String token = qrTokenUtil.generateVerificationToken(boardingPassId, boardingPassNumber, status);
        String extractedNumber = qrTokenUtil.extractBoardingPassNumber(token);

        assertThat(extractedNumber).isEqualTo(boardingPassNumber);
    }

    @Test
    void extractStatus_success() {
        Long boardingPassId = 123L;
        String boardingPassNumber = "BP123456";
        String status = "GENERATED";

        String token = qrTokenUtil.generateVerificationToken(boardingPassId, boardingPassNumber, status);
        String extractedStatus = qrTokenUtil.extractStatus(token);

        assertThat(extractedStatus).isEqualTo(status);
    }

    @Test
    void isTokenExpired_notExpired() {
        Long boardingPassId = 123L;
        String boardingPassNumber = "BP123456";
        String status = "GENERATED";

        String token = qrTokenUtil.generateVerificationToken(boardingPassId, boardingPassNumber, status);
        boolean isExpired = qrTokenUtil.isTokenExpired(token);

        assertThat(isExpired).isFalse();
    }

    @Test
    void isTokenExpired_invalid() {
        String invalidToken = "invalid.token.here";

        boolean isExpired = qrTokenUtil.isTokenExpired(invalidToken);

        assertThat(isExpired).isTrue();
    }

    @Test
    void generateVerificationToken_deterministic() {
        Long boardingPassId = 123L;
        String boardingPassNumber = "BP123456";
        String status = "GENERATED";

        String token1 = qrTokenUtil.generateVerificationToken(boardingPassId, boardingPassNumber, status);
        String token2 = qrTokenUtil.generateVerificationToken(boardingPassId, boardingPassNumber, status);

        // Tokens should be different due to timestamp
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void verifyToken_null() {
        Long verifiedId = qrTokenUtil.verifyToken(null);

        assertThat(verifiedId).isNull();
    }

    @Test
    void extractBoardingPassNumber_invalid() {
        String invalidToken = "invalid.token.here";

        String extractedNumber = qrTokenUtil.extractBoardingPassNumber(invalidToken);

        assertThat(extractedNumber).isNull();
    }

    @Test
    void extractStatus_invalid() {
        String invalidToken = "invalid.token.here";

        String extractedStatus = qrTokenUtil.extractStatus(invalidToken);

        assertThat(extractedStatus).isNull();
    }
}
