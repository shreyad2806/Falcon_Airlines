package com.falcon.airlines.common;

import com.falcon.airlines.entity.User;
import jakarta.persistence.*;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
