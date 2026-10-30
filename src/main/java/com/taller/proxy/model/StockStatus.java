package com.taller.proxy.model;

public record StockStatus(String sku, String name, int stock, int requested, boolean available) {
}
