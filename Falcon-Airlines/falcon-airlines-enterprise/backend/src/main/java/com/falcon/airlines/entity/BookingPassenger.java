package com.falcon.airlines.entity;

import com.falcon.airlines.common.AuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "booking_passengers")
@SQLRestriction("is_deleted = false")
public class BookingPassenger extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @Column(name = "fare_class", length = 1, nullable = false)
    private String fareClass;

    @Column(name = "cabin", length = 20, nullable = false)
    private String cabin;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "ssr_codes")
    private String[] ssrCodes;
}
