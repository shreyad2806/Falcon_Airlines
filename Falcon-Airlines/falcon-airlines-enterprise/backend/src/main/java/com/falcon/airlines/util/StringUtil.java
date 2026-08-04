package com.falcon.airlines.util;

/**
 * String utilities for sanitisation and formatting.
 */
public final class StringUtil {

    private StringUtil() {
    }

    public static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static boolean isEmpty(String value) {
        return value == null || value.isBlank();
    }
}
