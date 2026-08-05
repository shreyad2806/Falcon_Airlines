package com.falcon.airlines.entity;

import com.falcon.airlines.common.AuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "airports")
@SQLRestriction("is_deleted = false")
public class Airport extends AuditEntity {

    @Column(name = "iata_code", length = 3, nullable = false, unique = true)
    private String iataCode;

    @Column(name = "icao_code", length = 4, unique = true)
    private String icaoCode;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @Column(name = "country", length = 2, nullable = false)
    private String country;

    @Column(name = "time_zone", length = 50, nullable = false)
    private String timeZone;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
