package com.taller.proxy.exception;

import org.springframework.http.HttpStatus;

import java.util.Collection;

public class UnknownOperationException extends ServiceException {

    public UnknownOperationException(String serviceId, String operation, Collection<String> supported) {
        super(HttpStatus.NOT_FOUND,
                "Unknown operation '" + operation + "' for service '" + serviceId + "'. Supported: " + supported);
    }
}
