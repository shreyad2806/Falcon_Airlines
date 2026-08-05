package com.falcon.airlines.entity;

import com.falcon.airlines.common.AuditEntity;
import com.falcon.airlines.enums.FlightStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "flights")
@SQLRestriction("is_deleted = false")
public class Flight extends AuditEntity {

    @Column(name = "flight_number", length = 10, nullable = false)
    private String flightNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_airport_id", nullable = false)
    private Airport originAirport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_airport_id", nullable = false)
    private Airport destinationAirport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id", nullable = false)
    private Aircraft aircraft;

    @Column(name = "scheduled_departure", nullable = false)
    private Instant scheduledDeparture;

    @Column(name = "scheduled_arrival", nullable = false)
    private Instant scheduledArrival;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private FlightStatus status;

    @Column(name = "terminal", length = 10)
    private String terminal;

    @Column(name = "gate", length = 10)
    private String gate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
