package com.falcon.airlines.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enterprise roles. The {@code ROLE_} Spring Security prefix is added via {@link #getAuthority()}.
 */
public enum Role {
    ADMIN,
    CUSTOMER,
    AGENT;

    public String getName() {
        return name();
    }

    public String getAuthority() {
        return "ROLE_" + name();
    }

    public static Optional<Role> fromName(String name) {
        return Arrays.stream(values())
                .filter(r -> r.name().equalsIgnoreCase(name))
                .findFirst();
    }
}
