package com.falcon.airlines.entity;

import com.falcon.airlines.common.AuditEntity;
import com.falcon.airlines.enums.DocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "travel_documents")
@SQLRestriction("is_deleted = false")
public class TravelDocument extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 20, nullable = false)
    private DocumentType documentType;

    @Column(name = "document_number", length = 100, nullable = false)
    private String documentNumber;

    @Column(name = "issuing_country", length = 3, nullable = false)
    private String issuingCountry;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;
}
