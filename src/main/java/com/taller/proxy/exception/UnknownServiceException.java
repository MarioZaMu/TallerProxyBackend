package com.taller.proxy.exception;

import org.springframework.http.HttpStatus;

import java.util.Collection;

public class UnknownServiceException extends ServiceException {

    public UnknownServiceException(String serviceId, Collection<String> supported) {
        super(HttpStatus.NOT_FOUND, "Unknown service '" + serviceId + "'. Supported: " + supported);
    }
}
