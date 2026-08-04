package com.falcon.airlines.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Helpers for date and time formatting/parsing. Keeps date logic in one place
 * so the application uses consistent formats and time zones.
 */
public final class DateTimeUtil {

    public static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            .withZone(ZoneId.systemDefault());

    private DateTimeUtil() {
    }

    public static String format(Instant instant) {
        if (instant == null) {
            return null;
        }
        return ISO_OFFSET.format(instant);
    }

    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
