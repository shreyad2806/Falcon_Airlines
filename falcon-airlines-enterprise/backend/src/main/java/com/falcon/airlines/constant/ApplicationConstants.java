package com.falcon.airlines.constant;

/**
 * Centralised application constants. Keeps magic values and default keys in one
 * place so they can be reused and changed without scattering literals.
 */
public final class ApplicationConstants {

    private ApplicationConstants() {
    }

    public static final String API_BASE_PATH = "/api/v1";
    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
}
