package com.falcon.airlines.entity;

import com.falcon.airlines.common.AuditEntity;
import com.falcon.airlines.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "delay_predictions")
@SQLRestriction("is_deleted = false")
public class DelayPrediction extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(name = "prediction_time", nullable = false)
    private Instant predictionTime;

    @Column(name = "predicted_delay_minutes", nullable = false)
    private Integer predictedDelayMinutes;

    @Column(name = "probability", precision = 5, scale = 4, nullable = false)
    private BigDecimal probability;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20, nullable = false)
    private RiskLevel riskLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "factors")
    private String factors;

    @Column(name = "model_version", length = 50, nullable = false)
    private String modelVersion;
}
