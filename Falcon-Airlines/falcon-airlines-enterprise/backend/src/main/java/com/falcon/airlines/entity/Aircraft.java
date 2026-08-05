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
@Table(name = "aircraft")
@SQLRestriction("is_deleted = false")
public class Aircraft extends AuditEntity {

    @Column(name = "registration_number", length = 20, nullable = false, unique = true)
    private String registrationNumber;

    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "model", length = 100, nullable = false)
    private String model;

    @Column(name = "manufacturer", length = 100, nullable = false)
    private String manufacturer;

    @Column(name = "total_capacity", nullable = false)
    private Short totalCapacity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration")
    private String configuration;
}
