package com.falcon.airlines.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Fine-grained permissions for resource actions.
 * <p>
 * Each permission is stored as an authority string (e.g. {@code BOOKING_WRITE}) and can be
 * used with {@code hasAuthority('BOOKING_WRITE')} in {@code @PreAuthorize} expressions.
 */
public enum Permission {
    USER_READ,
    USER_WRITE,
    ROLE_READ,
    ROLE_WRITE,
    PERMISSION_READ,
    FLIGHT_READ,
    FLIGHT_WRITE,
    BOOKING_READ,
    BOOKING_WRITE,
    PASSENGER_READ,
    PASSENGER_WRITE,
    PAYMENT_READ,
    PAYMENT_WRITE,
    TICKET_READ,
    TICKET_WRITE;

    public String getCode() {
        return name();
    }

    public String getAuthority() {
        return getCode();
    }

    public static Optional<Permission> fromCode(String code) {
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(code))
                .findFirst();
    }
}
