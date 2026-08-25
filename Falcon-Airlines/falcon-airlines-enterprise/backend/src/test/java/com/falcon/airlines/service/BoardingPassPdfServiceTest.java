package com.falcon.airlines.service;

import com.falcon.airlines.entity.Airport;
import com.falcon.airlines.entity.Booking;
import com.falcon.airlines.entity.BoardingPass;
import com.falcon.airlines.entity.Flight;
import com.falcon.airlines.entity.Passenger;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.enums.BoardingPassStatus;
import com.falcon.airlines.enums.FlightStatus;
import com.falcon.airlines.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardingPassPdfServiceTest {

    @Mock
    private QrCodeService qrCodeService;

    private BoardingPassPdfService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new BoardingPassPdfService(qrCodeService);
    }

    @Test
    void generateBoardingPassPdf_success() {
        BoardingPass bp = buildFullBoardingPass();
        byte[] qrBytes = createDummyPng();
        when(qrCodeService.generateQrCodeBytes(anyString())).thenReturn(qrBytes);

        byte[] pdf = pdfService.generateBoardingPassPdf(bp);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
        // PDF magic bytes
        assertThat(new String(pdf, 0, 4)).startsWith("%PDF");
    }

    @Test
    void generateBoardingPassPdf_containsPdfHeader() {
        BoardingPass bp = buildFullBoardingPass();
        when(qrCodeService.generateQrCodeBytes(anyString())).thenReturn(createDummyPng());

        byte[] pdf = pdfService.generateBoardingPassPdf(bp);

        assertThat(pdf.length).isGreaterThan(1000);
    }

    @Test
    void generateBoardingPassPdf_withNullOptionalFields() {
        BoardingPass bp = buildMinimalBoardingPass();
        when(qrCodeService.generateQrCodeBytes(anyString())).thenReturn(createDummyPng());

        byte[] pdf = pdfService.generateBoardingPassPdf(bp);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
        assertThat(new String(pdf, 0, 4)).startsWith("%PDF");
    }

    @Test
    void generateBoardingPassPdf_qrFailureGracefulDegradation() {
        BoardingPass bp = buildFullBoardingPass();
        when(qrCodeService.generateQrCodeBytes(anyString()))
                .thenThrow(new RuntimeException("QR generation failed"));

        // Should not throw — should degrade gracefully
        byte[] pdf = pdfService.generateBoardingPassPdf(bp);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
        assertThat(new String(pdf, 0, 4)).startsWith("%PDF");
    }

    @Test
    void generateBoardingPassPdf_nullVerificationToken_qrFailure() {
        BoardingPass bp = buildFullBoardingPass();
        bp.setVerificationToken(null);
        when(qrCodeService.generateQrCodeBytes(null))
                .thenThrow(new NullPointerException("token is null"));

        // Should degrade gracefully
        byte[] pdf = pdfService.generateBoardingPassPdf(bp);

        assertThat(pdf).isNotNull();
        assertThat(new String(pdf, 0, 4)).startsWith("%PDF");
    }

    @Test
    void generateBoardingPassPdf_allStatuses() {
        for (BoardingPassStatus status : BoardingPassStatus.values()) {
            BoardingPass bp = buildFullBoardingPass();
            bp.setStatus(status);
            when(qrCodeService.generateQrCodeBytes(anyString())).thenReturn(createDummyPng());

            byte[] pdf = pdfService.generateBoardingPassPdf(bp);
            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
        }
    }

    @Test
    void generateBoardingPassPdf_largeQrCode() {
        BoardingPass bp = buildFullBoardingPass();
        // Simulate a large QR code
        byte[] largeQr = new byte[50000];
        java.util.Arrays.fill(largeQr, (byte) 0xFF);
        when(qrCodeService.generateQrCodeBytes(anyString())).thenReturn(largeQr);

        byte[] pdf = pdfService.generateBoardingPassPdf(bp);

        assertThat(pdf).isNotNull();
        assertThat(new String(pdf, 0, 4)).startsWith("%PDF");
    }

    @Test
    void generateBoardingPassPdf_emptyBoardingPassNumber() {
        BoardingPass bp = buildFullBoardingPass();
        bp.setBoardingPassNumber("");
        when(qrCodeService.generateQrCodeBytes(anyString())).thenReturn(createDummyPng());

        byte[] pdf = pdfService.generateBoardingPassPdf(bp);

        assertThat(pdf).isNotNull();
        assertThat(new String(pdf, 0, 4)).startsWith("%PDF");
    }

    // ── Helpers ──

    private byte[] createDummyPng() {
        // Minimal valid PNG (1x1 white pixel)
        return Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==");
    }

    private BoardingPass buildFullBoardingPass() {
        BoardingPass bp = new BoardingPass();
        bp.setId(1L);
        bp.setBoardingPassNumber("BP123456");
        bp.setStatus(BoardingPassStatus.GENERATED);
        bp.setSeatNumber("12A");
        bp.setSeatClass("ECONOMY");
        bp.setBoardingGroup("B");
        bp.setGate("A12");
        bp.setBoardingTime(Instant.now().plusSeconds(82800));
        bp.setVerificationToken("test-verification-token");
        bp.setQrCodePayload("BP:BP123456|TKT:TKT123456");
        bp.setGeneratedAt(Instant.now());
        bp.setVersion(0L);

        // Passenger
        Passenger passenger = new Passenger();
        passenger.setId(1L);
        passenger.setFirstName("John");
        passenger.setLastName("Doe");
        passenger.setEmail("john.doe@example.com");
        bp.setPassenger(passenger);

        // Flight
        Flight flight = new Flight();
        flight.setId(1L);
        flight.setFlightNumber("FA100");
        flight.setStatus(FlightStatus.SCHEDULED);
        flight.setScheduledDeparture(Instant.now().plusSeconds(86400));
        flight.setScheduledArrival(Instant.now().plusSeconds(172800));
        flight.setTerminal("T1");
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

        // Ticket
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTicketNumber("TKT123456");
        bp.setTicket(ticket);

        // Booking
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("BK789");
        bp.setBooking(booking);

        return bp;
    }

    private BoardingPass buildMinimalBoardingPass() {
        BoardingPass bp = new BoardingPass();
        bp.setId(2L);
        bp.setBoardingPassNumber("BP000001");
        bp.setStatus(BoardingPassStatus.GENERATED);
        bp.setVerificationToken("minimal-token");
        bp.setGeneratedAt(Instant.now());
        bp.setVersion(0L);
        // All optional fields left null
        return bp;
    }
}
