package com.taller.proxy.model;

import java.math.BigDecimal;

public record Product(String sku, String name, int stock, BigDecimal price) {
}
