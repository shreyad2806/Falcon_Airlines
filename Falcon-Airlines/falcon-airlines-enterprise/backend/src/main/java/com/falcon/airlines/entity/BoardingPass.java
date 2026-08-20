package com.falcon.airlines.entity;

import com.falcon.airlines.common.AuditEntity;
import com.falcon.airlines.enums.BoardingPassStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "boarding_passes")
@SQLRestriction("is_deleted = false")
public class BoardingPass extends AuditEntity {

    @Column(name = "boarding_pass_number", length = 20, nullable = false, unique = true)
    private String boardingPassNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "seat_number", length = 10)
    private String seatNumber;

    @Column(name = "seat_class", length = 20)
    private String seatClass;

    @Column(name = "boarding_group", length = 5)
    private String boardingGroup;

    @Column(name = "gate", length = 10)
    private String gate;

    @Column(name = "boarding_time")
    private Instant boardingTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private BoardingPassStatus status;

    @Column(name = "qr_code_payload", length = 500)
    private String qrCodePayload;

    @Column(name = "verification_token", length = 500)
    private String verificationToken;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "boarded_at")
    private Instant boardedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
