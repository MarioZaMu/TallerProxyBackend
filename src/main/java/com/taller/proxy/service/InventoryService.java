package com.taller.proxy.service;

import com.taller.proxy.config.SimulationProperties;
import com.taller.proxy.exception.InsufficientStockException;
import com.taller.proxy.exception.ResourceNotFoundException;
import com.taller.proxy.model.Product;
import com.taller.proxy.model.StockStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Inventory microservice. Stock changes are atomic per product. */
@Service
public class InventoryService extends SimulatedService {

    public static final String SERVICE_ID = "inventory";

    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public InventoryService(SimulationProperties properties) {
        super(SERVICE_ID, properties);
        seed();
        register("listProducts", p -> listProducts());
        register("checkStock", p -> checkStock(p.requireString("sku"), p.optionalPositiveInt("quantity", 1)));
        register("addStock", p -> addStock(p.requireString("sku"), p.requirePositiveInt("quantity")));
        register("removeStock", p -> removeStock(p.requireString("sku"), p.requirePositiveInt("quantity")));
    }

    public List<Product> listProducts() {
        return products.values().stream().sorted(Comparator.comparing(Product::sku)).toList();
    }

    public StockStatus checkStock(String sku, int requested) {
        Product product = find(sku);
        return new StockStatus(product.sku(), product.name(), product.stock(), requested,
                product.stock() >= requested);
    }

    public Product addStock(String sku, int quantity) {
        find(sku);
        return products.computeIfPresent(sku,
                (key, current) -> new Product(key, current.name(), current.stock() + quantity, current.price()));
    }

    public Product removeStock(String sku, int quantity) {
        find(sku);
        return products.computeIfPresent(sku, (key, current) -> {
            if (current.stock() < quantity) {
                throw new InsufficientStockException(key, current.stock(), quantity);
            }
            return new Product(key, current.name(), current.stock() - quantity, current.price());
        });
    }

    /** Used by the order service (logical reservation). */
    public Product reserve(String sku, int quantity) {
        return removeStock(sku, quantity);
    }

    /** Returns previously reserved units to the stock. */
    public Product release(String sku, int quantity) {
        return addStock(sku, quantity);
    }

    public Product find(String sku) {
        Product product = products.get(sku);
        if (product == null) {
            throw new ResourceNotFoundException("Product not found: " + sku);
        }
        return product;
    }

    private void seed() {
        add("SKU-1001", "Laptop Pro 14\"", 40, "4599000");
        add("SKU-1002", "Wireless Mouse", 150, "89000");
        add("SKU-1003", "Mechanical Keyboard", 90, "259000");
        add("SKU-1004", "27\" 4K Monitor", 35, "1299000");
        add("SKU-1005", "USB-C Dock", 70, "349000");
        add("SKU-1006", "Noise-Cancelling Headphones", 55, "799000");
        add("SKU-1007", "HD Webcam", 120, "189000");
        add("SKU-1008", "External SSD 1TB", 80, "459000");
    }

    private void add(String sku, String name, int stock, String price) {
        products.put(sku, new Product(sku, name, stock, new BigDecimal(price)));
    }
}
