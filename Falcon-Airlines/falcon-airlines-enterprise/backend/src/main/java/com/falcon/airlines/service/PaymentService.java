package com.falcon.airlines.service;

import com.falcon.airlines.dto.request.PaymentRequest;
import com.falcon.airlines.dto.response.PaymentResponse;
import com.falcon.airlines.entity.Booking;
import com.falcon.airlines.entity.Payment;
import com.falcon.airlines.entity.Seat;
import com.falcon.airlines.entity.SeatAllocation;
import com.falcon.airlines.entity.SeatHold;
import com.falcon.airlines.entity.Ticket;
import com.falcon.airlines.enums.BookingPaymentStatus;
import com.falcon.airlines.enums.BookingStatus;
import com.falcon.airlines.enums.TicketStatus;
import com.falcon.airlines.exception.BaseException;
import com.falcon.airlines.repository.BookingRepository;
import com.falcon.airlines.repository.PaymentRepository;
import com.falcon.airlines.repository.SeatAllocationRepository;
import com.falcon.airlines.repository.SeatHoldRepository;
import com.falcon.airlines.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class PaymentService {

    private static final int HOLD_DURATION_MINUTES = 15;

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatAllocationRepository seatAllocationRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          TicketRepository ticketRepository,
                          SeatHoldRepository seatHoldRepository,
                          SeatAllocationRepository seatAllocationRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.seatAllocationRepository = seatAllocationRepository;
    }

    /**
     * Create a seat hold when user selects a seat in the booking flow.
     * Expires after 15 minutes.
     */
    public SeatHold holdSeat(Long flightId, Long seatId, Long bookingId, Long userId) {
        log.info("Holding seat {} for flight {}, user {}", seatId, flightId, userId);

        // Check if seat is already held by someone else
        var existingHold = seatHoldRepository.findActiveHoldForSeat(seatId, flightId);
        if (existingHold.isPresent()) {
            SeatHold hold = existingHold.get();
            if (!hold.getHeldByUser().getId().equals(userId)) {
                throw new BaseException("Seat is currently being reserved by another user", HttpStatus.CONFLICT, "SEAT_HELD_BY_OTHER");
            }
            // Same user — refresh hold
            hold.setHoldExpiresAt(Instant.now().plus(HOLD_DURATION_MINUTES, ChronoUnit.MINUTES));
            return seatHoldRepository.save(hold);
        }

        // Check if seat is already allocated (booked)
        boolean isAllocated = seatAllocationRepository.existsBySeatIdAndFlightId(seatId, flightId);
        if (isAllocated) {
            throw new BaseException("Seat is already booked", HttpStatus.CONFLICT, "SEAT_ALREADY_BOOKED");
        }

        SeatHold hold = new SeatHold();
        hold.setFlight(new com.falcon.airlines.entity.Flight());
        hold.getFlight().setId(flightId);
        hold.setSeat(new Seat());
        hold.getSeat().setId(seatId);
        if (bookingId != null) {
            hold.setBooking(new Booking());
            hold.getBooking().setId(bookingId);
        }
        hold.setHeldByUser(new com.falcon.airlines.entity.User());
        hold.getHeldByUser().setId(userId);
        hold.setHoldExpiresAt(Instant.now().plus(HOLD_DURATION_MINUTES, ChronoUnit.MINUTES));
        hold.setStatus("HELD");

        return seatHoldRepository.save(hold);
    }

    /**
     * Release a seat hold.
     */
    public void releaseSeatHold(Long flightId, Long seatId, Long userId) {
        log.info("Releasing seat hold for seat {} on flight {}, user {}", seatId, flightId, userId);
        seatHoldRepository.findActiveHoldForSeat(seatId, flightId)
                .filter(h -> h.getHeldByUser().getId().equals(userId))
                .ifPresent(hold -> {
                    hold.setStatus("RELEASED");
                    seatHoldRepository.save(hold);
                });
    }

    /**
     * Process demo payment. Simulates successful payment for demo purposes.
     */
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for booking: {}, method: {}", request.getBookingId(), request.getPaymentMethod());

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BaseException("Booking not found", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BaseException("Booking is not awaiting payment", HttpStatus.BAD_REQUEST, "BOOKING_NOT_PENDING_PAYMENT");
        }

        // Check seat holds are still valid
        var holds = seatHoldRepository.findByBookingIdAndStatusAndIsDeletedFalse(booking.getId(), "HELD");
        for (SeatHold hold : holds) {
            if (hold.getHoldExpiresAt().isBefore(Instant.now())) {
                hold.setStatus("EXPIRED");
                seatHoldRepository.save(hold);
                throw new BaseException("Seat reservation has expired. Please start a new booking.", HttpStatus.GONE, "SEAT_HOLD_EXPIRED");
            }
        }

        // Create payment record
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setTransactionId(transactionId);
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency(booking.getCurrency());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus("SUCCESS");
        payment.setGatewayReference("DEMO-" + transactionId);
        payment.setPaidAt(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);

        // Update booking
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(BookingPaymentStatus.PAID);
        bookingRepository.save(booking);

        // Mark seat holds as COMPLETED
        for (SeatHold hold : holds) {
            hold.setStatus("COMPLETED");
            seatHoldRepository.save(hold);
        }

        // Mark tickets as ACTIVE
        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.ACTIVE || ticket.getStatus() == TicketStatus.ISSUED) {
                ticket.setStatus(TicketStatus.ACTIVE);
                ticketRepository.save(ticket);
            }
        }

        log.info("Payment successful: {}, booking: {} confirmed", transactionId, booking.getBookingReference());

        return mapToPaymentResponse(savedPayment, booking);
    }

    /**
     * Simulate payment failure for demo.
     */
    public PaymentResponse simulateFailure(Long bookingId) {
        log.info("Simulating payment failure for booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BaseException("Booking not found", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BaseException("Booking is not awaiting payment", HttpStatus.BAD_REQUEST, "BOOKING_NOT_PENDING_PAYMENT");
        }

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setTransactionId(transactionId);
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency(booking.getCurrency());
        payment.setPaymentMethod("UPI");
        payment.setStatus("FAILED");
        payment.setGatewayReference("DEMO-FAIL-" + transactionId);

        paymentRepository.save(payment);

        // Release seat holds
        var holds = seatHoldRepository.findByBookingIdAndStatusAndIsDeletedFalse(booking.getId(), "HELD");
        for (SeatHold hold : holds) {
            hold.setStatus("RELEASED");
            seatHoldRepository.save(hold);
        }

        // Update booking status
        booking.setStatus(BookingStatus.PAYMENT_FAILED);
        bookingRepository.save(booking);

        // Cancel tickets
        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        for (Ticket ticket : tickets) {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
        }

        return mapToPaymentResponse(payment, booking);
    }

    /**
     * Release all expired seat holds.
     */
    @Transactional
    public void releaseExpiredHolds() {
        List<SeatHold> expired = seatHoldRepository.findExpiredHolds();
        for (SeatHold hold : expired) {
            hold.setStatus("EXPIRED");
            seatHoldRepository.save(hold);
            log.info("Released expired seat hold: seat {} on flight {}", hold.getSeat().getId(), hold.getFlight().getId());
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId).stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()))
                .findFirst()
                .orElseThrow(() -> new BaseException("No successful payment found", HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BaseException("Booking not found", HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND"));

        return mapToPaymentResponse(payment, booking);
    }

    private PaymentResponse mapToPaymentResponse(Payment payment, Booking booking) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(payment.getId());
        response.setBookingId(booking.getId());
        response.setBookingReference(booking.getBookingReference());
        response.setTransactionId(payment.getTransactionId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setStatus(payment.getStatus());
        response.setGatewayReference(payment.getGatewayReference());
        response.setPaidAt(payment.getPaidAt());
        response.setBookingStatus(booking.getStatus().name());
        return response;
    }
}
