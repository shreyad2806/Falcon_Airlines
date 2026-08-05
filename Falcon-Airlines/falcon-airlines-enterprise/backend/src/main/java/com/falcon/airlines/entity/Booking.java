package com.falcon.airlines.entity;

import com.falcon.airlines.common.AuditEntity;
import com.falcon.airlines.enums.BookingPaymentStatus;
import com.falcon.airlines.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "bookings")
@SQLRestriction("is_deleted = false")
public class Booking extends AuditEntity {

    @Column(name = "booking_reference", length = 10, nullable = false, unique = true)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private BookingStatus status;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "booking_date", nullable = false)
    private Instant bookingDate;

    @Column(name = "time_limit")
    private Instant timeLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20, nullable = false)
    private BookingPaymentStatus paymentStatus;
}
