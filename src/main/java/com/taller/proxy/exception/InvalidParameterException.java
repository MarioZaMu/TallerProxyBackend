package com.taller.proxy.exception;

import org.springframework.http.HttpStatus;

public class InvalidParameterException extends ServiceException {

    public InvalidParameterException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
