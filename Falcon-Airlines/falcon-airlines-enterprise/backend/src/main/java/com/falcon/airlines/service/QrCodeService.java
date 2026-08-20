package com.falcon.airlines.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/**
 * Service for generating QR codes from verification tokens.
 */
@Slf4j
@Service
public class QrCodeService {

    private static final int QR_CODE_SIZE = 300;
    private static final String IMAGE_FORMAT = "PNG";

    /**
     * Generate a QR code image from the verification token.
     * Returns the QR code as a base64-encoded PNG image.
     */
    public String generateQrCodeBase64(String verificationToken) {
        try {
            BufferedImage qrImage = generateQrCodeImage(verificationToken);
            return encodeImageToBase64(qrImage);
        } catch (Exception e) {
            log.error("Failed to generate QR code for token", e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Generate a QR code image from the verification token.
     * Returns the QR code as a byte array (PNG format).
     */
    public byte[] generateQrCodeBytes(String verificationToken) {
        try {
            BufferedImage qrImage = generateQrCodeImage(verificationToken);
            return encodeImageToBytes(qrImage);
        } catch (Exception e) {
            log.error("Failed to generate QR code for token", e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Generate a QR code BufferedImage from the verification token.
     */
    private BufferedImage generateQrCodeImage(String verificationToken) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.MARGIN, 1,
                EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
        );

        BitMatrix bitMatrix = qrCodeWriter.encode(
                verificationToken,
                BarcodeFormat.QR_CODE,
                QR_CODE_SIZE,
                QR_CODE_SIZE,
                hints
        );

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * Encode a BufferedImage to a base64 string.
     */
    private String encodeImageToBase64(BufferedImage image) throws IOException {
        byte[] imageBytes = encodeImageToBytes(image);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * Encode a BufferedImage to a byte array (PNG format).
     */
    private byte[] encodeImageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, IMAGE_FORMAT, outputStream);
        return outputStream.toByteArray();
    }
}
