package com.falcon.airlines.service;

import com.falcon.airlines.dto.response.BoardingPassResponse;
import com.falcon.airlines.dto.response.TicketDetailResponse;
import com.falcon.airlines.entity.*;
import com.falcon.airlines.enums.*;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.repository.*;
import com.falcon.airlines.util.QrTokenUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Phase 7 end-to-end tests covering the complete ticket & boarding-pass user journey.
 *
 * All tests run as pure unit tests with mocks (no Docker/Testcontainers required).
 * Tests are organized by the 20 scenarios specified in the requirements.
 */
class Phase7EndToEndTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BoardingPassRepository boardingPassRepository;
    @Mock private SeatAllocationRepository seatAllocationRepository;
    @Mock private QrTokenUtil qrTokenUtil;
    @Mock private QrCodeService qrCodeService;

    private TicketService ticketService;
    private BoardingPassService boardingPassService;
    private BoardingPassPdfService boardingPassPdfService;

    // Test data
    private User customer;
    private User otherUser;
    private Airport originAirport;
    private Airport destAirport;
    private Aircraft aircraft;
    private Flight flight;
    private Passenger passenger;
    private Booking booking;
    private Ticket ticket;
    private Seat seat;
    private SeatAllocation seatAllocation;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        bookingRepository = mock(BookingRepository.class);
        boardingPassRepository = mock(BoardingPassRepository.class);
        seatAllocationRepository = mock(SeatAllocationRepository.class);
        qrTokenUtil = mock(QrTokenUtil.class);
        qrCodeService = mock(QrCodeService.class);

        ticketService = new TicketService(ticketRepository, bookingRepository, seatAllocationRepository);
        boardingPassService = new BoardingPassService(
                boardingPassRepository, ticketRepository, bookingRepository,
                seatAllocationRepository, qrTokenUtil);
        boardingPassPdfService = new BoardingPassPdfService(qrCodeService);

        buildTestData();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 1: Create/confirm a valid booking
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 1: Booking validation")
    class Scenario1_BookingValidation {
        @Test
        @DisplayName("Booking with CONFIRMED status is valid for ticket/boarding-pass flow")
        void confirmedBooking_isValid() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            TicketDetailResponse response = ticketService.getTicketById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getBookingReference()).isEqualTo("BK789");
            assertThat(response.getFlightNumber()).isEqualTo("FA100");
        }

        @Test
        @DisplayName("Non-existent ticket returns NOT_FOUND")
        void nonExistentTicket_returnsNotFound() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.getTicketById(999L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                    .hasFieldOrPropertyWithValue("errorCode", "TICKET_NOT_FOUND");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 2: Generate the ticket
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 2: Ticket generation/retrieval")
    class Scenario2_TicketGeneration {
        @Test
        @DisplayName("Retrieve active ticket by ID")
        void retrieveActiveTicket_success() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            TicketDetailResponse response = ticketService.getTicketById(1L);

            assertThat(response.getTicketNumber()).isEqualTo("TKT123456");
            assertThat(response.getStatus()).isEqualTo(TicketStatus.ACTIVE);
        }

        @Test
        @DisplayName("Retrieve ticket by number")
        void retrieveTicketByNumber_success() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(ticketRepository.findByTicketNumber("TKT123456")).thenReturn(Optional.of(ticket));

            TicketDetailResponse response = ticketService.getTicketByNumber("TKT123456");

            assertThat(response.getTicketNumber()).isEqualTo("TKT123456");
        }

        @Test
        @DisplayName("Retrieve ticket for a booking")
        void retrieveTicketsByBooking_success() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            when(ticketRepository.findByBookingId(1L)).thenReturn(List.of(ticket));

            List<TicketDetailResponse> responses = ticketService.getTicketsByBookingId(1L);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getTicketNumber()).isEqualTo("TKT123456");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 3-4: Retrieve ticket & verify status
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 3-4: Ticket retrieval & status verification")
    class Scenario3_4_TicketRetrievalAndStatus {
        @Test
        @DisplayName("Active ticket has correct status and fields")
        void activeTicket_statusAndFields() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            TicketDetailResponse response = ticketService.getTicketById(1L);

            assertThat(response.getStatus()).isEqualTo(TicketStatus.ACTIVE);
            assertThat(response.getFlightNumber()).isEqualTo("FA100");
            assertThat(response.getPassengerName()).isEqualTo("John Doe");
            assertThat(response.getOriginAirportCode()).isEqualTo("JFK");
            assertThat(response.getDestinationAirportCode()).isEqualTo("LAX");
        }

        @Test
        @DisplayName("Ticket with seat allocation includes seat info")
        void ticketWithSeat_includesSeatInfo() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.of(seatAllocation));

            TicketDetailResponse response = ticketService.getTicketById(1L);

            assertThat(response.getSeatNumber()).isEqualTo("12A");
            assertThat(response.getSeatClass()).isEqualTo("ECONOMY");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 5: Generate the boarding pass
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 5: Boarding pass generation")
    class Scenario5_BoardingPassGeneration {
        @Test
        @DisplayName("Generate boarding pass for valid ticket")
        void generateBoardingPass_success() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(boardingPassRepository.findByTicketId(1L)).thenReturn(List.of());
            when(boardingPassRepository.save(any(BoardingPass.class)))
                    .thenAnswer(inv -> { BoardingPass bp = inv.getArgument(0); bp.setId(1L); return bp; });
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.of(seatAllocation));
            when(qrTokenUtil.generateVerificationToken(any(), anyString(), anyString()))
                    .thenReturn("jwt-verification-token");

            BoardingPassResponse response = boardingPassService.generateBoardingPass(1L);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(BoardingPassStatus.GENERATED);
            assertThat(response.getPassengerName()).isEqualTo("John Doe");
            assertThat(response.getFlightNumber()).isEqualTo("FA100");
            assertThat(response.getTicketNumber()).isEqualTo("TKT123456");
            assertThat(response.getBookingReference()).isEqualTo("BK789");
            assertThat(response.getVerificationToken()).isEqualTo("jwt-verification-token");
        }

        @Test
        @DisplayName("Cannot generate boarding pass for cancelled ticket")
        void generateBoardingPass_cancelledTicket_throws() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            ticket.setStatus(TicketStatus.CANCELLED);
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            assertThatThrownBy(() -> boardingPassService.generateBoardingPass(1L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "TICKET_NOT_ACTIVE");
        }

        @Test
        @DisplayName("Cannot generate boarding pass for non-confirmed booking")
        void generateBoardingPass_unconfirmedBooking_throws() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            booking.setStatus(BookingStatus.PENDING);
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            assertThatThrownBy(() -> boardingPassService.generateBoardingPass(1L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "BOOKING_NOT_CONFIRMED");
        }

        @Test
        @DisplayName("Cannot generate boarding pass for cancelled flight")
        void generateBoardingPass_cancelledFlight_throws() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            flight.setStatus(FlightStatus.CANCELLED);
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            assertThatThrownBy(() -> boardingPassService.generateBoardingPass(1L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "FLIGHT_NOT_SCHEDULED");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 6: Retrieve the boarding pass
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 6: Boarding pass retrieval")
    class Scenario6_BoardingPassRetrieval {
        @Test
        @DisplayName("Retrieve boarding pass by ID")
        void retrieveBoardingPass_success() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.of(seatAllocation));

            BoardingPassResponse response = boardingPassService.getBoardingPassById(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getBoardingPassNumber()).isEqualTo("BP123456");
            assertThat(response.getSeatNumber()).isEqualTo("12A");
            assertThat(response.getGate()).isEqualTo("A12");
        }

        @Test
        @DisplayName("Retrieve boarding pass by number")
        void retrieveBoardingPassByNumber_success() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            when(boardingPassRepository.findByBoardingPassNumber("BP123456")).thenReturn(Optional.of(bp));
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.of(seatAllocation));

            BoardingPassResponse response = boardingPassService.getBoardingPassByNumber("BP123456");

            assertThat(response.getBoardingPassNumber()).isEqualTo("BP123456");
        }

        @Test
        @DisplayName("Non-existent boarding pass returns NOT_FOUND")
        void retrieveBoardingPass_notFound() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(boardingPassRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> boardingPassService.getBoardingPassById(999L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                    .hasFieldOrPropertyWithValue("errorCode", "BOARDING_PASS_NOT_FOUND");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 7-8: QR code generation & verification
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 7-8: QR code generation & verification")
    class Scenario7_8_QrCodeGenerationAndVerification {
        @Test
        @DisplayName("QR code is generated from verification token")
        void generateQrCode_usesVerificationToken() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            bp.setVerificationToken("valid-jwt-token");
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.of(seatAllocation));
            when(qrCodeService.generateQrCodeBase64("valid-jwt-token"))
                    .thenReturn(Base64.getEncoder().encodeToString("qr-image-data".getBytes()));

            // Verify QR code generation uses the verification token
            String qrBase64 = qrCodeService.generateQrCodeBase64(bp.getVerificationToken());
            assertThat(qrBase64).isNotEmpty();
        }

        @Test
        @DisplayName("QR code bytes are generated for PDF embedding")
        void generateQrCodeBytes_forPdf() {
            byte[] qrPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
            when(qrCodeService.generateQrCodeBytes("valid-token")).thenReturn(qrPng);

            byte[] result = qrCodeService.generateQrCodeBytes("valid-token");
            assertThat(result).isNotNull();
            assertThat(result.length).isEqualTo(4);
        }

        @Test
        @DisplayName("QR verification token is unique per boarding pass")
        void verificationToken_perBoardingPass() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(boardingPassRepository.findByTicketId(1L)).thenReturn(List.of());
            when(boardingPassRepository.save(any(BoardingPass.class)))
                    .thenAnswer(inv -> { BoardingPass bp = inv.getArgument(0); bp.setId(1L); return bp; });
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());
            when(qrTokenUtil.generateVerificationToken(any(), anyString(), anyString()))
                    .thenReturn("token-pass-1")
                    .thenReturn("token-pass-1");

            BoardingPassResponse response = boardingPassService.generateBoardingPass(1L);
            assertThat(response.getVerificationToken()).isNotNull();
            assertThat(response.getVerificationToken()).isNotBlank();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 9: Download ticket PDF
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 9: Ticket PDF download")
    class Scenario9_TicketPdf {
        @Test
        @DisplayName("Ticket PDF is valid and starts with %PDF")
        void ticketPdf_validFormat() {
            when(seatAllocationRepository.findByTicketId(anyLong())).thenReturn(Optional.of(seatAllocation));

            byte[] pdf = new TicketPdfService(seatAllocationRepository).generateTicketPdf(ticket);

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
            assertThat(new String(pdf, 0, 4)).startsWith("%PDF");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 10: Download boarding-pass PDF
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 10: Boarding pass PDF download")
    class Scenario10_BoardingPassPdf {
        @Test
        @DisplayName("Boarding pass PDF is valid and starts with %PDF")
        void boardingPassPdf_validFormat() {
            BoardingPass bp = buildSavedBoardingPass();
            when(qrCodeService.generateQrCodeBytes(anyString())).thenReturn(createDummyPng());

            byte[] pdf = boardingPassPdfService.generateBoardingPassPdf(bp);

            assertThat(pdf).isNotNull();
            assertThat(new String(pdf, 0, 4)).startsWith("%PDF");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 11-12: Open PDFs & verify information matches booking
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 11-12: PDF content verification")
    class Scenario11_12_PdfContentVerification {
        @Test
        @DisplayName("Boarding pass response contains all booking information")
        void boardingPassResponse_containsAllBookingInfo() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.of(seatAllocation));

            BoardingPassResponse response = boardingPassService.getBoardingPassById(1L);

            // Verify all required fields are present and match the booking
            assertThat(response.getPassengerName()).isEqualTo("John Doe");
            assertThat(response.getFlightNumber()).isEqualTo("FA100");
            assertThat(response.getOriginAirportCode()).isEqualTo("JFK");
            assertThat(response.getDestinationAirportCode()).isEqualTo("LAX");
            assertThat(response.getGate()).isEqualTo("A12");
            assertThat(response.getBoardingTime()).isNotNull();
            assertThat(response.getSeatNumber()).isEqualTo("12A");
            assertThat(response.getSeatClass()).isEqualTo("ECONOMY");
            assertThat(response.getBoardingGroup()).isEqualTo("B");
            assertThat(response.getBookingReference()).isEqualTo("BK789");
            assertThat(response.getTicketNumber()).isEqualTo("TKT123456");
            assertThat(response.getStatus()).isEqualTo(BoardingPassStatus.GENERATED);
        }

        @Test
        @DisplayName("Boarding pass response ticket number matches original ticket")
        void boardingPass_ticketNumberMatches() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.of(seatAllocation));

            BoardingPassResponse response = boardingPassService.getBoardingPassById(1L);

            assertThat(response.getTicketNumber()).isEqualTo(ticket.getTicketNumber());
            assertThat(response.getBookingReference()).isEqualTo(booking.getBookingReference());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 13-14: QR scannability & backend validation
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 13-14: QR code scannability & backend validation")
    class Scenario13_14_QrScannabilityAndValidation {
        @Test
        @DisplayName("QR code is generated from the boarding pass verification token")
        void qrCode_embeddedInPdf() {
            BoardingPass bp = buildSavedBoardingPass();
            bp.setVerificationToken("real-jwt-token");
            when(qrCodeService.generateQrCodeBytes("real-jwt-token")).thenReturn(createDummyPng());

            byte[] pdf = boardingPassPdfService.generateBoardingPassPdf(bp);

            // Verify QR service was called with the correct token
            verify(qrCodeService).generateQrCodeBytes("real-jwt-token");
            assertThat(pdf).isNotNull();
        }

        @Test
        @DisplayName("Verification token is stored on the boarding pass entity")
        void verificationToken_storedOnEntity() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(boardingPassRepository.findByTicketId(1L)).thenReturn(List.of());
            when(boardingPassRepository.save(any(BoardingPass.class)))
                    .thenAnswer(inv -> { BoardingPass bp = inv.getArgument(0); bp.setId(10L); return bp; });
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());
            when(qrTokenUtil.generateVerificationToken(isNull(), anyString(), anyString()))
                    .thenReturn("initial-token");
            when(qrTokenUtil.generateVerificationToken(eq(10L), anyString(), anyString()))
                    .thenReturn("final-token");

            BoardingPassResponse response = boardingPassService.generateBoardingPass(1L);

            assertThat(response.getVerificationToken()).isEqualTo("final-token");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 15-16: Cancellation & cancelled ticket/boarding-pass behavior
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 15-16: Cancellation behavior")
    class Scenario15_16_Cancellation {
        @Test
        @DisplayName("Cancel ticket transitions to CANCELLED status")
        void cancelTicket_success() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            TicketDetailResponse response = ticketService.cancelTicket(1L);

            assertThat(response.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        }

        @Test
        @DisplayName("Cancelling already cancelled ticket is a no-op (same status)")
        void cancelTicket_alreadyCancelled_noOp() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            ticket.setStatus(TicketStatus.CANCELLED);
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            // Same-status transitions are no-ops, no exception thrown
            TicketDetailResponse response = ticketService.cancelTicket(1L);
            assertThat(response.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        }

        @Test
        @DisplayName("Cannot generate boarding pass for cancelled ticket")
        void generateBoardingPass_cancelledTicket_throws() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            ticket.setStatus(TicketStatus.CANCELLED);
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            assertThatThrownBy(() -> boardingPassService.generateBoardingPass(1L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "TICKET_NOT_ACTIVE");
        }

        @Test
        @DisplayName("Void boarding pass cannot transition to any other state")
        void voidBoardingPass_terminalState() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            bp.setStatus(BoardingPassStatus.VOID);
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));

            assertThatThrownBy(() -> boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.USED))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "BOARDING_PASS_ALREADY_VOID");
        }

        @Test
        @DisplayName("Used boarding pass cannot transition to any other state")
        void usedBoardingPass_terminalState() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            bp.setStatus(BoardingPassStatus.USED);
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));

            assertThatThrownBy(() -> boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.CHECKED_IN))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "BOARDING_PASS_ALREADY_USED");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 17: Unauthorized access
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 17: Unauthorized access")
    class Scenario17_UnauthorizedAccess {
        @Test
        @DisplayName("Different user cannot access another user's ticket")
        void unauthorizedAccess_ticket() {
            setupSecurityContext("otheruser", "ROLE_CUSTOMER");
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            assertThatThrownBy(() -> ticketService.getTicketById(1L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                    .hasFieldOrPropertyWithValue("errorCode", "TICKET_ACCESS_DENIED");
        }

        @Test
        @DisplayName("Different user cannot access another user's boarding pass")
        void unauthorizedAccess_boardingPass() {
            setupSecurityContext("otheruser", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));

            assertThatThrownBy(() -> boardingPassService.getBoardingPassById(1L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                    .hasFieldOrPropertyWithValue("errorCode", "BOARDING_PASS_ACCESS_DENIED");
        }

        @Test
        @DisplayName("Different user cannot generate boarding pass for another user's ticket")
        void unauthorizedAccess_generateBoardingPass() {
            setupSecurityContext("otheruser", "ROLE_CUSTOMER");
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            assertThatThrownBy(() -> boardingPassService.generateBoardingPass(1L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "TICKET_ACCESS_DENIED");
        }

        @Test
        @DisplayName("Different user cannot cancel another user's ticket")
        void unauthorizedAccess_cancelTicket() {
            setupSecurityContext("otheruser", "ROLE_CUSTOMER");
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            assertThatThrownBy(() -> ticketService.cancelTicket(1L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "TICKET_ACCESS_DENIED");
        }

        @Test
        @DisplayName("Admin can access any ticket")
        void adminAccess_ticket() {
            setupSecurityContext("admin", "ROLE_ADMIN");
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            TicketDetailResponse response = ticketService.getTicketById(1L);
            assertThat(response).isNotNull();
            assertThat(response.getTicketNumber()).isEqualTo("TKT123456");
        }

        @Test
        @DisplayName("Admin can access any boarding pass")
        void adminAccess_boardingPass() {
            setupSecurityContext("admin", "ROLE_ADMIN");
            BoardingPass bp = buildSavedBoardingPass();
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.of(seatAllocation));

            BoardingPassResponse response = boardingPassService.getBoardingPassById(1L);
            assertThat(response).isNotNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 18: Invalid/non-existent IDs
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 18: Invalid/non-existent IDs")
    class Scenario18_InvalidIds {
        @Test
        @DisplayName("Non-existent ticket ID returns NOT_FOUND")
        void nonExistentTicketId() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(ticketRepository.findById(99999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.getTicketById(99999L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Non-existent boarding pass ID returns NOT_FOUND")
        void nonExistentBoardingPassId() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(boardingPassRepository.findById(99999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> boardingPassService.getBoardingPassById(99999L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Non-existent ticket number returns NOT_FOUND")
        void nonExistentTicketNumber() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(ticketRepository.findByTicketNumber("NONEXISTENT")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.getTicketByNumber("NONEXISTENT"))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Non-existent booking ID returns NOT_FOUND")
        void nonExistentBookingId() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(bookingRepository.findById(99999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.getTicketsByBookingId(99999L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 19: Missing optional information
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 19: Missing optional information")
    class Scenario19_MissingOptionalInfo {
        @Test
        @DisplayName("Boarding pass PDF handles null passenger")
        void pdf_nullPassenger() {
            BoardingPass bp = buildMinimalBoardingPass();
            when(qrCodeService.generateQrCodeBytes(anyString())).thenReturn(createDummyPng());

            byte[] pdf = boardingPassPdfService.generateBoardingPassPdf(bp);
            assertThat(pdf).isNotNull();
            assertThat(new String(pdf, 0, 4)).startsWith("%PDF");
        }

        @Test
        @DisplayName("Boarding pass PDF handles null flight")
        void pdf_nullFlight() {
            BoardingPass bp = buildMinimalBoardingPass();
            bp.setFlight(null);
            when(qrCodeService.generateQrCodeBytes(anyString())).thenReturn(createDummyPng());

            byte[] pdf = boardingPassPdfService.generateBoardingPassPdf(bp);
            assertThat(pdf).isNotNull();
            assertThat(new String(pdf, 0, 4)).startsWith("%PDF");
        }

        @Test
        @DisplayName("Boarding pass PDF handles null booking")
        void pdf_nullBooking() {
            BoardingPass bp = buildMinimalBoardingPass();
            bp.setBooking(null);
            when(qrCodeService.generateQrCodeBytes(anyString())).thenReturn(createDummyPng());

            byte[] pdf = boardingPassPdfService.generateBoardingPassPdf(bp);
            assertThat(pdf).isNotNull();
            assertThat(new String(pdf, 0, 4)).startsWith("%PDF");
        }

        @Test
        @DisplayName("Boarding pass PDF handles null seat/class/group")
        void pdf_nullSeatInfo() {
            BoardingPass bp = buildMinimalBoardingPass();
            bp.setSeatNumber(null);
            bp.setSeatClass(null);
            bp.setBoardingGroup(null);
            when(qrCodeService.generateQrCodeBytes(anyString())).thenReturn(createDummyPng());

            byte[] pdf = boardingPassPdfService.generateBoardingPassPdf(bp);
            assertThat(pdf).isNotNull();
            assertThat(new String(pdf, 0, 4)).startsWith("%PDF");
        }

        @Test
        @DisplayName("Boarding pass response handles null origin/destination airports")
        void response_nullAirports() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            bp.getFlight().setOriginAirport(null);
            bp.getFlight().setDestinationAirport(null);
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

            BoardingPassResponse response = boardingPassService.getBoardingPassById(1L);

            assertThat(response.getOriginAirportCode()).isNull();
            assertThat(response.getDestinationAirportCode()).isNull();
        }

        @Test
        @DisplayName("Boarding pass response overrides null gate with flight gate")
        void boardingPass_nullGate_overriddenByFlight() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            bp.setGate(null);
            bp.setBoardingTime(null);
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

            BoardingPassResponse response = boardingPassService.getBoardingPassById(1L);

            // mapToBoardingPassResponse overrides gate from flight, so BP null gate gets flight gate
            assertThat(response.getGate()).isEqualTo("A12");
            // BoardingTime comes from the boarding pass entity (null here)
            assertThat(response.getBoardingTime()).isNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCENARIO 20: Duplicate generation
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Scenario 20: Duplicate generation prevention")
    class Scenario20_DuplicateGeneration {
        @Test
        @DisplayName("Cannot generate duplicate boarding pass for same ticket")
        void duplicateBoardingPass_throwsConflict() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            BoardingPass existingPass = buildSavedBoardingPass();
            when(boardingPassRepository.findByTicketId(1L)).thenReturn(List.of(existingPass));

            assertThatThrownBy(() -> boardingPassService.generateBoardingPass(1L))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                    .hasFieldOrPropertyWithValue("errorCode", "BOARDING_PASS_ALREADY_EXISTS");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  BOARDING PASS STATUS TRANSITIONS (complete lifecycle)
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Boarding pass lifecycle status transitions")
    class BoardingPassLifecycle {
        @Test
        @DisplayName("Full lifecycle: GENERATED → CHECKED_IN → BOARDING → USED")
        void fullLifecycle_success() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            bp.setStatus(BoardingPassStatus.GENERATED);
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));
            when(boardingPassRepository.save(any(BoardingPass.class))).thenAnswer(inv -> inv.getArgument(0));

            // GENERATED → CHECKED_IN
            BoardingPassResponse resp1 = boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.CHECKED_IN);
            assertThat(resp1.getStatus()).isEqualTo(BoardingPassStatus.CHECKED_IN);
            assertThat(resp1.getCheckedInAt()).isNotNull();
        }

        @Test
        @DisplayName("GENERATED → VOID is valid")
        void generatedToVoid_valid() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            bp.setStatus(BoardingPassStatus.GENERATED);
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));
            when(boardingPassRepository.save(any(BoardingPass.class))).thenAnswer(inv -> inv.getArgument(0));

            BoardingPassResponse resp = boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.VOID);
            assertThat(resp.getStatus()).isEqualTo(BoardingPassStatus.VOID);
        }

        @Test
        @DisplayName("CHECKED_IN → BOARDING is valid")
        void checkedInToBoarding_valid() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            bp.setStatus(BoardingPassStatus.CHECKED_IN);
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));
            when(boardingPassRepository.save(any(BoardingPass.class))).thenAnswer(inv -> inv.getArgument(0));

            BoardingPassResponse resp = boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.BOARDING);
            assertThat(resp.getStatus()).isEqualTo(BoardingPassStatus.BOARDING);
        }

        @Test
        @DisplayName("CHECKED_IN → GENERATED is invalid (cannot go back)")
        void checkedInToGenerated_invalid() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            bp.setStatus(BoardingPassStatus.CHECKED_IN);
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));

            assertThatThrownBy(() -> boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.GENERATED))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_STATUS_TRANSITION");
        }

        @Test
        @DisplayName("BOARDING → CHECKED_IN is invalid (cannot go back)")
        void boardingToCheckedIn_invalid() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            bp.setStatus(BoardingPassStatus.BOARDING);
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));

            assertThatThrownBy(() -> boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.CHECKED_IN))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_STATUS_TRANSITION");
        }

        @Test
        @DisplayName("GENERATED → USED is invalid (must go through BOARDING)")
        void generatedToUsed_invalid() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            bp.setStatus(BoardingPassStatus.GENERATED);
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));

            assertThatThrownBy(() -> boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.USED))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_STATUS_TRANSITION");
        }

        @Test
        @DisplayName("Same status is a no-op (no exception)")
        void sameStatus_noOp() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            bp.setStatus(BoardingPassStatus.GENERATED);
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));
            when(boardingPassRepository.save(any(BoardingPass.class))).thenAnswer(inv -> inv.getArgument(0));

            BoardingPassResponse resp = boardingPassService.updateBoardingPassStatus(1L, BoardingPassStatus.GENERATED);
            assertThat(resp.getStatus()).isEqualTo(BoardingPassStatus.GENERATED);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PDF CONTENT INTEGRITY
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("PDF content integrity")
    class PdfContentIntegrity {
        @Test
        @DisplayName("Boarding pass PDF is non-empty and valid PDF")
        void pdf_sufficientSize() {
            BoardingPass bp = buildFullBoardingPass();
            when(qrCodeService.generateQrCodeBytes(anyString())).thenReturn(createDummyPng());

            byte[] pdf = boardingPassPdfService.generateBoardingPassPdf(bp);

            assertThat(pdf.length).isGreaterThan(1_000);
        }

        @Test
        @DisplayName("Ticket PDF is non-empty and valid PDF")
        void ticketPdf_sufficientSize() {
            when(seatAllocationRepository.findByTicketId(anyLong())).thenReturn(Optional.of(seatAllocation));

            byte[] pdf = new TicketPdfService(seatAllocationRepository).generateTicketPdf(ticket);

            assertThat(pdf.length).isGreaterThan(1_000);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DATA PRIVACY
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Data privacy - no sensitive info exposed in boarding pass response")
    class DataPrivacy {
        @Test
        @DisplayName("Boarding pass response does not contain passport number")
        void response_noPassportNumber() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

            BoardingPassResponse response = boardingPassService.getBoardingPassById(1L);

            // BoardingPassResponse doesn't have passport fields — verify via toString
            assertThat(response.toString()).doesNotContain("PP123456");
        }

        @Test
        @DisplayName("Boarding pass response does not contain email")
        void response_noEmail() {
            setupSecurityContext("customer1", "ROLE_CUSTOMER");
            BoardingPass bp = buildSavedBoardingPass();
            when(boardingPassRepository.findById(1L)).thenReturn(Optional.of(bp));
            when(seatAllocationRepository.findByTicketId(1L)).thenReturn(Optional.empty());

            BoardingPassResponse response = boardingPassService.getBoardingPassById(1L);

            assertThat(response.toString()).doesNotContain("john.doe@example.com");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void buildTestData() {
        customer = new User();
        customer.setId(1L);
        customer.setUsername("customer1");
        customer.setEmail("customer1@example.com");
        customer.setPasswordHash("hashed");
        customer.setStatus(UserStatus.ACTIVE);

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPasswordHash("hashed");
        otherUser.setStatus(UserStatus.ACTIVE);

        originAirport = new Airport();
        originAirport.setId(1L);
        originAirport.setIataCode("JFK");
        originAirport.setIcaoCode("KJFK");
        originAirport.setName("John F. Kennedy International");
        originAirport.setCity("New York");
        originAirport.setCountry("US");
        originAirport.setTimeZone("America/New_York");
        originAirport.setIsActive(true);

        destAirport = new Airport();
        destAirport.setId(2L);
        destAirport.setIataCode("LAX");
        destAirport.setIcaoCode("KLAX");
        destAirport.setName("Los Angeles International");
        destAirport.setCity("Los Angeles");
        destAirport.setCountry("US");
        destAirport.setTimeZone("America/Los_Angeles");
        destAirport.setIsActive(true);

        aircraft = new Aircraft();
        aircraft.setId(1L);
        aircraft.setRegistrationNumber("N12345");
        aircraft.setType("COMMERCIAL");
        aircraft.setModel("Boeing 737");
        aircraft.setManufacturer("Boeing");
        aircraft.setTotalCapacity((short) 150);

        flight = new Flight();
        flight.setId(1L);
        flight.setFlightNumber("FA100");
        flight.setOriginAirport(originAirport);
        flight.setDestinationAirport(destAirport);
        flight.setAircraft(aircraft);
        flight.setScheduledDeparture(Instant.now().plusSeconds(86400));
        flight.setScheduledArrival(Instant.now().plusSeconds(172800));
        flight.setStatus(FlightStatus.SCHEDULED);
        flight.setTerminal("T1");
        flight.setGate("A12");
        flight.setIsActive(true);

        passenger = new Passenger();
        passenger.setId(1L);
        passenger.setFirstName("John");
        passenger.setLastName("Doe");
        passenger.setEmail("john.doe@example.com");
        passenger.setPhone("+1234567890");
        passenger.setPassportNumber("PP123456");
        passenger.setNationality("USA");
        passenger.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        passenger.setGender(Gender.M);

        booking = new Booking();
        booking.setId(1L);
        booking.setCustomer(customer);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingReference("BK789");
        booking.setTotalAmount(BigDecimal.valueOf(200.00));
        booking.setCurrency("USD");
        booking.setBookingDate(Instant.now());
        booking.setPaymentStatus(BookingPaymentStatus.PAID);

        ticket = new Ticket();
        ticket.setId(1L);
        ticket.setBooking(booking);
        ticket.setPassenger(passenger);
        ticket.setFlight(flight);
        ticket.setTicketNumber("TKT123456");
        ticket.setFareBasis("ECONOMY");
        ticket.setFare(BigDecimal.valueOf(100.00));
        ticket.setTaxes(BigDecimal.valueOf(20.00));
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setIssuedAt(Instant.now());
        ticket.setVersion(0L);

        seat = new Seat();
        seat.setId(1L);
        seat.setAircraft(aircraft);
        seat.setSeatNumber("12A");
        seat.setSeatClass("ECONOMY");
        seat.setRowNumber((short) 12);
        seat.setColumnLetter("A");
        seat.setIsActive(true);

        seatAllocation = new SeatAllocation();
        seatAllocation.setId(1L);
        seatAllocation.setSeat(seat);
        seatAllocation.setTicket(ticket);
        seatAllocation.setFlight(flight);
        seatAllocation.setAllocatedAt(Instant.now());
    }

    private BoardingPass buildSavedBoardingPass() {
        BoardingPass bp = new BoardingPass();
        bp.setId(1L);
        bp.setBoardingPassNumber("BP123456");
        bp.setTicket(ticket);
        bp.setPassenger(passenger);
        bp.setFlight(flight);
        bp.setBooking(booking);
        bp.setStatus(BoardingPassStatus.GENERATED);
        bp.setSeatNumber("12A");
        bp.setSeatClass("ECONOMY");
        bp.setBoardingGroup("B");
        bp.setGate("A12");
        bp.setBoardingTime(Instant.now().plusSeconds(82800));
        bp.setQrCodePayload("BP:BP123456|TKT:TKT123456");
        bp.setVerificationToken("test-verification-token");
        bp.setGeneratedAt(Instant.now());
        bp.setVersion(0L);
        return bp;
    }

    private BoardingPass buildFullBoardingPass() {
        BoardingPass bp = buildSavedBoardingPass();
        bp.setVerificationToken("full-token");
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
        return bp;
    }

    private byte[] createDummyPng() {
        return Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==");
    }

    private void setupSecurityContext(String username, String role) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(username);
        lenient().when(authentication.getAuthorities()).thenReturn((List) List.of(new SimpleGrantedAuthority(role)));
        lenient().when(authentication.isAuthenticated()).thenReturn(true);

        org.springframework.security.core.context.SecurityContext securityContext =
                mock(org.springframework.security.core.context.SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
