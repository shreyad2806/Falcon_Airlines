package com.falcon.airlines.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Adds creator/updater tracking to entities. Combined with {@link BaseEntity}
 * it provides the full audit column contract defined in the database design.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AuditEntity extends BaseEntity {

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
