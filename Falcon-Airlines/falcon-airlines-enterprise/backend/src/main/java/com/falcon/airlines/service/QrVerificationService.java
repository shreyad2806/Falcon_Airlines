package com.falcon.airlines.service;

import com.falcon.airlines.dto.response.BoardingPassResponse;
import com.falcon.airlines.entity.BoardingPass;
import com.falcon.airlines.enums.BoardingPassStatus;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.repository.BoardingPassRepository;
import com.falcon.airlines.util.QrTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for verifying QR code tokens for boarding passes.
 */
@Slf4j
@Service
public class QrVerificationService {

    private final BoardingPassRepository boardingPassRepository;
    private final QrTokenUtil qrTokenUtil;

    public QrVerificationService(BoardingPassRepository boardingPassRepository,
                                   QrTokenUtil qrTokenUtil) {
        this.boardingPassRepository = boardingPassRepository;
        this.qrTokenUtil = qrTokenUtil;
    }

    /**
     * Verify a QR code token and return boarding pass details if valid.
     * 
     * Verification process:
     * 1. Parse and validate the JWT token
     * 2. Check if token is expired
     * 3. Retrieve the boarding pass from the token
     * 4. Verify the boarding pass exists and is not void
     * 5. Return boarding pass details
     */
    public Map<String, Object> verifyQrToken(String token) {
        log.info("Verifying QR token");

        // Check if token is expired
        if (qrTokenUtil.isTokenExpired(token)) {
            throw new BaseException("QR token has expired", HttpStatus.BAD_REQUEST, "QR_TOKEN_EXPIRED");
        }

        // Extract boarding pass ID from token
        Long boardingPassId = qrTokenUtil.verifyToken(token);
        if (boardingPassId == null) {
            throw new BaseException("Invalid QR token", HttpStatus.BAD_REQUEST, "INVALID_QR_TOKEN");
        }

        // Retrieve boarding pass
        BoardingPass boardingPass = boardingPassRepository.findById(boardingPassId)
                .orElseThrow(() -> new BaseException("Boarding pass not found", HttpStatus.NOT_FOUND, "BOARDING_PASS_NOT_FOUND"));

        // Verify boarding pass is not void
        if (boardingPass.getStatus() == BoardingPassStatus.VOID) {
            throw new BaseException("Boarding pass has been voided", HttpStatus.BAD_REQUEST, "BOARDING_PASS_VOID");
        }

        // Verify the token matches the stored verification token (prevents token reuse)
        if (!token.equals(boardingPass.getVerificationToken())) {
            throw new BaseException("QR token does not match boarding pass", HttpStatus.BAD_REQUEST, "QR_TOKEN_MISMATCH");
        }

        log.info("QR token verified successfully for boarding pass: {}", boardingPassId);

        // Return verification result
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("boardingPassId", boardingPass.getId());
        result.put("boardingPassNumber", boardingPass.getBoardingPassNumber());
        result.put("status", boardingPass.getStatus());
        result.put("passengerName", boardingPass.getPassenger() != null ? 
                boardingPass.getPassenger().getFirstName() + " " + boardingPass.getPassenger().getLastName() : "Unknown");
        result.put("flightNumber", boardingPass.getFlight() != null ? boardingPass.getFlight().getFlightNumber() : "Unknown");
        result.put("seatNumber", boardingPass.getSeatNumber());
        result.put("gate", boardingPass.getGate());
        result.put("boardingTime", boardingPass.getBoardingTime());

        return result;
    }

    /**
     * Quick verification - returns only validity status without detailed information.
     * Useful for quick checks at boarding gates.
     */
    public boolean isQrTokenValid(String token) {
        try {
            verifyQrToken(token);
            return true;
        } catch (Exception e) {
            log.warn("QR token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
