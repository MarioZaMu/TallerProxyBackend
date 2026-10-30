package com.taller.proxy.model;

import java.math.BigDecimal;

public record OrderItem(String sku, String name, int quantity, BigDecimal unitPrice) {
}
