package com.falcon.airlines.entity;

import com.falcon.airlines.common.AuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "permissions")
@SQLRestriction("is_deleted = false")
public class Permission extends AuditEntity {

    @Column(name = "code", length = 100, nullable = false, unique = true)
    private String code;

    @Column(name = "description")
    private String description;

    @Column(name = "resource", length = 50, nullable = false)
    private String resource;

    @Column(name = "action", length = 50, nullable = false)
    private String action;
}
