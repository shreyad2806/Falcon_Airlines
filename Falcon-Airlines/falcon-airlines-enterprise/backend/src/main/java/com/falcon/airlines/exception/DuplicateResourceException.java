package com.falcon.airlines.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request would create a resource that already exists
 * and has a unique business-key (e.g., username, email).
 */
public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }
}
