package com.taller.proxy.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Payment(
        String paymentId,
        String orderId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public Payment withStatus(PaymentStatus newStatus) {
        return new Payment(paymentId, orderId, amount, method, newStatus, createdAt, Instant.now());
    }
}
