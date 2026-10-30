package com.taller.proxy.exception;

import org.springframework.http.HttpStatus;

/** Base class of domain errors; each one knows the HTTP status it maps to. */
public abstract class ServiceException extends RuntimeException {

    private final HttpStatus status;

    protected ServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
