package com.falcon.airlines.entity;

import com.falcon.airlines.common.AuditEntity;
import com.falcon.airlines.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "tickets")
@SQLRestriction("is_deleted = false")
public class Ticket extends AuditEntity {

    @Column(name = "ticket_number", length = 20, nullable = false, unique = true)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(name = "fare_basis", length = 10, nullable = false)
    private String fareBasis;

    @Column(name = "fare", precision = 15, scale = 2, nullable = false)
    private BigDecimal fare;

    @Column(name = "taxes", precision = 15, scale = 2, nullable = false)
    private BigDecimal taxes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TicketStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;
}
