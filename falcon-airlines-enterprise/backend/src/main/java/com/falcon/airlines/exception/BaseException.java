package com.falcon.airlines.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Parent runtime exception for all domain and infrastructure errors.
 * Business exceptions will extend this class in later phases.
 */
@Getter
public class BaseException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public BaseException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public BaseException(String message, HttpStatus status, String errorCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}
