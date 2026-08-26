package com.falcon.airlines.entity;

import com.falcon.airlines.common.AuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "seats")
@SQLRestriction("is_deleted = false")
public class Seat extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id", nullable = false)
    private Aircraft aircraft;

    @Column(name = "seat_number", length = 10, nullable = false)
    private String seatNumber;

    @Column(name = "seat_class", length = 20, nullable = false)
    private String seatClass;

    @Column(name = "row_number")
    private Short rowNumber;

    @Column(name = "column_letter", length = 1)
    private String columnLetter;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "price", precision = 10, scale = 2)
    private java.math.BigDecimal price;

    @Column(name = "seat_type", length = 20)
    private String seatType;
}
