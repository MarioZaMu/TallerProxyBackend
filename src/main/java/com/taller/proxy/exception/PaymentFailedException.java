package com.taller.proxy.exception;

import org.springframework.http.HttpStatus;

/** Raised when the payment gateway rejects or fails a call (including the intentional random failures). */
public class PaymentFailedException extends ServiceException {

    public PaymentFailedException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}
