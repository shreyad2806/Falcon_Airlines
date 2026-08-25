package com.falcon.airlines.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrCodeServiceTest {

    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeService();
    }

    @Test
    void generateQrCodeBase64_success() {
        String verificationToken = "test-token-12345";
        
        String qrCodeBase64 = qrCodeService.generateQrCodeBase64(verificationToken);
        
        assertThat(qrCodeBase64).isNotNull();
        assertThat(qrCodeBase64).isNotEmpty();
        // Base64 encoded PNG should start with specific pattern
        assertThat(qrCodeBase64).startsWith("iVBORw0KGgo");
    }

    @Test
    void generateQrCodeBytes_success() {
        String verificationToken = "test-token-12345";
        
        byte[] qrCodeBytes = qrCodeService.generateQrCodeBytes(verificationToken);
        
        assertThat(qrCodeBytes).isNotNull();
        assertThat(qrCodeBytes).isNotEmpty();
        // PNG file signature
        assertThat(qrCodeBytes[0]).isEqualTo((byte) 0x89);
        assertThat(qrCodeBytes[1]).isEqualTo((byte) 0x50);
        assertThat(qrCodeBytes[2]).isEqualTo((byte) 0x4E);
        assertThat(qrCodeBytes[3]).isEqualTo((byte) 0x47);
    }

    @Test
    void generateQrCodeBase64_deterministic() {
        String verificationToken = "test-token-12345";
        
        String qrCode1 = qrCodeService.generateQrCodeBase64(verificationToken);
        String qrCode2 = qrCodeService.generateQrCodeBase64(verificationToken);
        
        // Same input should produce same output
        assertThat(qrCode1).isEqualTo(qrCode2);
    }

    @Test
    void generateQrCodeBase64_nullToken() {
        assertThatThrownBy(() -> qrCodeService.generateQrCodeBase64(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate QR code");
    }
}
