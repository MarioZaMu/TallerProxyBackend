package com.taller.proxy.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Order(
        String orderId,
        String customerId,
        List<OrderItem> items,
        BigDecimal total,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public Order withStatus(OrderStatus newStatus) {
        return new Order(orderId, customerId, items, total, newStatus, createdAt, Instant.now());
    }
}
