package com.taller.proxy.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ServiceException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
