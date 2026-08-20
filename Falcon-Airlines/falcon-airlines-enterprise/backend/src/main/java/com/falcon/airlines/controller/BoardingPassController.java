package com.falcon.airlines.controller;

import com.falcon.airlines.dto.response.BoardingPassResponse;
import com.falcon.airlines.enums.BoardingPassStatus;
import com.falcon.airlines.response.ApiResponse;
import com.falcon.airlines.service.BoardingPassService;
import com.falcon.airlines.service.QrCodeService;
import com.falcon.airlines.service.QrVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for boarding pass management.
 */
@Tag(name = "Boarding Pass Management", description = "Boarding pass generation and status management operations")
@RestController
@RequestMapping("/api/boarding-passes")
public class BoardingPassController {

    private final BoardingPassService boardingPassService;
    private final QrCodeService qrCodeService;
    private final QrVerificationService qrVerificationService;

    public BoardingPassController(BoardingPassService boardingPassService,
                                  QrCodeService qrCodeService,
                                  QrVerificationService qrVerificationService) {
        this.boardingPassService = boardingPassService;
        this.qrCodeService = qrCodeService;
        this.qrVerificationService = qrVerificationService;
    }

    @Operation(summary = "Generate boarding pass for a ticket")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/ticket/{ticketId}")
    @PreAuthorize("hasAnyAuthority('BOARDING_PASS_WRITE', 'BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<BoardingPassResponse>> generateBoardingPass(@PathVariable Long ticketId) {
        BoardingPassResponse response = boardingPassService.generateBoardingPass(ticketId);
        return ResponseEntity.ok(ApiResponse.ok("Boarding pass generated successfully", response));
    }

    @Operation(summary = "Get boarding pass by ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('BOARDING_PASS_READ', 'BOOKING_READ')")
    public ResponseEntity<ApiResponse<BoardingPassResponse>> getBoardingPassById(@PathVariable Long id) {
        BoardingPassResponse response = boardingPassService.getBoardingPassById(id);
        return ResponseEntity.ok(ApiResponse.ok("Boarding pass retrieved successfully", response));
    }

    @Operation(summary = "Get boarding pass by boarding pass number")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/number/{boardingPassNumber}")
    @PreAuthorize("hasAnyAuthority('BOARDING_PASS_READ', 'BOOKING_READ')")
    public ResponseEntity<ApiResponse<BoardingPassResponse>> getBoardingPassByNumber(@PathVariable String boardingPassNumber) {
        BoardingPassResponse response = boardingPassService.getBoardingPassByNumber(boardingPassNumber);
        return ResponseEntity.ok(ApiResponse.ok("Boarding pass retrieved successfully", response));
    }

    @Operation(summary = "Get all boarding passes for a booking")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyAuthority('BOARDING_PASS_READ', 'BOOKING_READ')")
    public ResponseEntity<ApiResponse<List<BoardingPassResponse>>> getBoardingPassesByBookingId(@PathVariable Long bookingId) {
        List<BoardingPassResponse> response = boardingPassService.getBoardingPassesByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.ok("Boarding passes retrieved successfully", response));
    }

    @Operation(summary = "Get all boarding passes for a passenger")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/passenger/{passengerId}")
    @PreAuthorize("hasAnyAuthority('BOARDING_PASS_READ', 'BOOKING_READ')")
    public ResponseEntity<ApiResponse<List<BoardingPassResponse>>> getBoardingPassesByPassengerId(@PathVariable Long passengerId) {
        List<BoardingPassResponse> response = boardingPassService.getBoardingPassesByPassengerId(passengerId);
        return ResponseEntity.ok(ApiResponse.ok("Boarding passes retrieved successfully", response));
    }

    @Operation(summary = "Update boarding pass status")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('BOARDING_PASS_WRITE', 'BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<BoardingPassResponse>> updateBoardingPassStatus(
            @PathVariable Long id,
            @Parameter(description = "New boarding pass status") @RequestParam BoardingPassStatus status) {
        BoardingPassResponse response = boardingPassService.updateBoardingPassStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Boarding pass status updated successfully", response));
    }

    @Operation(summary = "Check in passenger")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAnyAuthority('BOARDING_PASS_WRITE', 'BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<BoardingPassResponse>> checkInBoardingPass(@PathVariable Long id) {
        BoardingPassResponse response = boardingPassService.checkInBoardingPass(id);
        return ResponseEntity.ok(ApiResponse.ok("Passenger checked in successfully", response));
    }

    @Operation(summary = "Mark passenger as boarded")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/board")
    @PreAuthorize("hasAnyAuthority('BOARDING_PASS_WRITE', 'BOOKING_WRITE')")
    public ResponseEntity<ApiResponse<BoardingPassResponse>> boardPassenger(@PathVariable Long id) {
        BoardingPassResponse response = boardingPassService.boardPassenger(id);
        return ResponseEntity.ok(ApiResponse.ok("Passenger boarded successfully", response));
    }

    @Operation(summary = "Generate QR code for boarding pass")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/qr-code")
    @PreAuthorize("hasAnyAuthority('BOARDING_PASS_READ', 'BOOKING_READ')")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateQrCode(@PathVariable Long id) {
        BoardingPassResponse boardingPass = boardingPassService.getBoardingPassById(id);
        String qrCodeBase64 = qrCodeService.generateQrCodeBase64(boardingPass.getVerificationToken());
        
        Map<String, String> response = Map.of(
                "qrCode", qrCodeBase64,
                "format", "PNG",
                "encoding", "BASE64"
        );
        
        return ResponseEntity.ok(ApiResponse.ok("QR code generated successfully", response));
    }

    @Operation(summary = "Verify QR code token")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/verify")
    @PreAuthorize("hasAnyAuthority('BOARDING_PASS_READ', 'BOOKING_READ')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyQrToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Token is required"));
        }
        
        Map<String, Object> verificationResult = qrVerificationService.verifyQrToken(token);
        return ResponseEntity.ok(ApiResponse.ok("QR token verified successfully", verificationResult));
    }
}
