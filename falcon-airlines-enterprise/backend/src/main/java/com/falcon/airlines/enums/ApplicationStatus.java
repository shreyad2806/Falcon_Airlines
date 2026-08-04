package com.falcon.airlines.enums;

/**
 * High-level status values reused across multiple entities.
 * Domain-specific status enums will be added in later phases.
 */
public enum ApplicationStatus {
    ACTIVE,
    INACTIVE,
    PENDING,
    DELETED,
    ARCHIVED
}
