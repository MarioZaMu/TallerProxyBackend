package com.taller.proxy.exception;

public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(String sku, int available, int requested) {
        super("Insufficient stock for " + sku + ": available " + available + ", requested " + requested);
    }
}
