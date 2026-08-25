package com.falcon.airlines.controller;

import com.falcon.airlines.config.SecurityConfig;
import com.falcon.airlines.entity.*;
import com.falcon.airlines.enums.*;
import com.falcon.airlines.security.jwt.JwtAuthenticationFilter;
import com.falcon.airlines.security.jwt.JwtService;
import com.falcon.airlines.security.jwt.JwtTokenUtil;
import com.falcon.airlines.service.BoardingPassPdfService;
import com.falcon.airlines.service.BoardingPassService;
import com.falcon.airlines.service.QrCodeService;
import com.falcon.airlines.service.QrVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the boarding-pass PDF download REST endpoint.
 * Uses @WebMvcTest (slice test) so no Testcontainers/Docker required.
 */
@WebMvcTest(BoardingPassController.class)
@Import({SecurityConfig.class, JwtTokenUtil.class, JwtService.class, JwtAuthenticationFilter.class})
class BoardingPassControllerPdfTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardingPassService boardingPassService;

    @MockBean
    private QrCodeService qrCodeService;

    @MockBean
    private QrVerificationService qrVerificationService;

    @MockBean
    private BoardingPassPdfService boardingPassPdfService;

    @MockBean
    private UserDetailsService userDetailsService;

    private BoardingPass testBoardingPass;

    @BeforeEach
    void setUp() {
        testBoardingPass = buildTestBoardingPass();
    }

    // ── Scenario 10: Download boarding-pass PDF ──

    @Test
    @WithMockUser(authorities = "BOARDING_PASS_READ")
    void downloadBoardingPassPdf_success() throws Exception {
        when(boardingPassService.getBoardingPassEntityById(1L)).thenReturn(testBoardingPass);
        byte[] fakePdf = "%PDF-1.4 fake-content".getBytes();
        when(boardingPassPdfService.generateBoardingPassPdf(any(BoardingPass.class))).thenReturn(fakePdf);

        mockMvc.perform(get("/api/boarding-passes/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @WithMockUser(authorities = "BOOKING_READ")
    void downloadBoardingPassPdf_withBookingAuth_success() throws Exception {
        when(boardingPassService.getBoardingPassEntityById(1L)).thenReturn(testBoardingPass);
        byte[] fakePdf = "%PDF-1.4 content".getBytes();
        when(boardingPassPdfService.generateBoardingPassPdf(any(BoardingPass.class))).thenReturn(fakePdf);

        mockMvc.perform(get("/api/boarding-passes/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void downloadBoardingPassPdf_noAuth_returns4xx() throws Exception {
        // Spring Security returns 403 when no auth is provided and PreAuthorize is used
        mockMvc.perform(get("/api/boarding-passes/1/pdf"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status)
                            .isIn(401, 403);
                });
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_AUTH")
    void downloadBoardingPassPdf_wrongAuthority_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/boarding-passes/1/pdf"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "BOARDING_PASS_READ")
    void downloadBoardingPassPdf_notFound() throws Exception {
        when(boardingPassService.getBoardingPassEntityById(999L))
                .thenThrow(new com.falcon.airlines.exception.BaseException(
                        "Boarding pass not found",
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "BOARDING_PASS_NOT_FOUND"));

        mockMvc.perform(get("/api/boarding-passes/999/pdf"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "BOARDING_PASS_READ")
    void downloadBoardingPassPdf_contentDispositionHeader() throws Exception {
        when(boardingPassService.getBoardingPassEntityById(1L)).thenReturn(testBoardingPass);
        byte[] fakePdf = "%PDF".getBytes();
        when(boardingPassPdfService.generateBoardingPassPdf(any(BoardingPass.class))).thenReturn(fakePdf);

        mockMvc.perform(get("/api/boarding-passes/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("boarding_pass_BP123456.pdf")));
    }

    // ── Scenario 7: QR code endpoint ──

    @Test
    @WithMockUser(authorities = "BOARDING_PASS_READ")
    void generateQrCode_success() throws Exception {
        com.falcon.airlines.dto.response.BoardingPassResponse bpResponse =
                com.falcon.airlines.dto.response.BoardingPassResponse.builder()
                        .id(1L)
                        .boardingPassNumber("BP123456")
                        .verificationToken("test-token")
                        .build();
        when(boardingPassService.getBoardingPassById(1L)).thenReturn(bpResponse);
        when(qrCodeService.generateQrCodeBase64("test-token"))
                .thenReturn(Base64.getEncoder().encodeToString("qr-data".getBytes()));

        mockMvc.perform(get("/api/boarding-passes/1/qr-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.qrCode").isNotEmpty())
                .andExpect(jsonPath("$.data.format").value("PNG"))
                .andExpect(jsonPath("$.data.encoding").value("BASE64"));
    }

    // ── Helpers ──

    private BoardingPass buildTestBoardingPass() {
        BoardingPass bp = new BoardingPass();
        bp.setId(1L);
        bp.setBoardingPassNumber("BP123456");
        bp.setStatus(BoardingPassStatus.GENERATED);
        bp.setSeatNumber("12A");
        bp.setSeatClass("ECONOMY");
        bp.setBoardingGroup("B");
        bp.setGate("A12");
        bp.setBoardingTime(Instant.now().plusSeconds(82800));
        bp.setVerificationToken("test-token");
        bp.setGeneratedAt(Instant.now());
        bp.setVersion(0L);

        Passenger passenger = new Passenger();
        passenger.setId(1L);
        passenger.setFirstName("John");
        passenger.setLastName("Doe");
        bp.setPassenger(passenger);

        Flight flight = new Flight();
        flight.setId(1L);
        flight.setFlightNumber("FA100");
        flight.setStatus(FlightStatus.SCHEDULED);
        flight.setScheduledDeparture(Instant.now().plusSeconds(86400));
        flight.setGate("A12");

        Airport origin = new Airport();
        origin.setIataCode("JFK");
        origin.setCity("New York");
        flight.setOriginAirport(origin);

        Airport dest = new Airport();
        dest.setIataCode("LAX");
        dest.setCity("Los Angeles");
        flight.setDestinationAirport(dest);

        bp.setFlight(flight);

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTicketNumber("TKT123456");
        bp.setTicket(ticket);

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("BK789");
        bp.setBooking(booking);

        return bp;
    }
}
