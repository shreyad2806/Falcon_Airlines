package com.falcon.airlines.entity;

import com.falcon.airlines.common.AuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "roles")
@SQLRestriction("is_deleted = false")
public class Role extends AuditEntity {

    @Column(name = "name", length = 50, nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem;
}
