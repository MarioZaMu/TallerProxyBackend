package com.taller.proxy.service;

import com.taller.proxy.config.SimulationProperties;
import com.taller.proxy.exception.InsufficientStockException;
import com.taller.proxy.exception.InvalidParameterException;
import com.taller.proxy.exception.ResourceNotFoundException;
import com.taller.proxy.exception.UnknownOperationException;
import com.taller.proxy.model.Product;
import com.taller.proxy.model.StockStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryServiceTest {

    private final InventoryService service = new InventoryService(new SimulationProperties(0, 0, 0));

    @Test
    @SuppressWarnings("unchecked")
    void listProductsReturnsSeedData() {
        List<Product> products = (List<Product>) service.execute("listProducts", Map.of());
        assertEquals(8, products.size());
    }

    @Test
    void checkStockReportsAvailability() {
        StockStatus ok = (StockStatus) service.execute("checkStock", Map.of("sku", "SKU-1001", "quantity", 5));
        StockStatus tooMany = (StockStatus) service.execute("checkStock", Map.of("sku", "SKU-1001", "quantity", 9999));
        assertTrue(ok.available());
        assertFalse(tooMany.available());
    }

    @Test
    void addAndRemoveStockChangeTheAmount() {
        int before = service.find("SKU-1002").stock();
        Product added = (Product) service.execute("addStock", Map.of("sku", "SKU-1002", "quantity", 10));
        assertEquals(before + 10, added.stock());
        Product removed = (Product) service.execute("removeStock", Map.of("sku", "SKU-1002", "quantity", 4));
        assertEquals(before + 6, removed.stock());
    }

    @Test
    void removingMoreThanAvailableFails() {
        assertThrows(InsufficientStockException.class,
                () -> service.execute("removeStock", Map.of("sku", "SKU-1001", "quantity", 100000)));
    }

    @Test
    void validatesInput() {
        assertThrows(ResourceNotFoundException.class, () -> service.execute("checkStock", Map.of("sku", "NOPE")));
        assertThrows(InvalidParameterException.class, () -> service.execute("addStock", Map.of("sku", "SKU-1001")));
        assertThrows(InvalidParameterException.class,
                () -> service.execute("addStock", Map.of("sku", "SKU-1001", "quantity", -2)));
        assertThrows(UnknownOperationException.class, () -> service.execute("explode", Map.of()));
    }

    @Test
    void concurrentRemovalsNeverOversell() throws InterruptedException {
        int stock = service.find("SKU-1004").stock();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        AtomicInteger succeeded = new AtomicInteger();
        for (int i = 0; i < stock + 50; i++) {
            pool.submit(() -> {
                try {
                    service.removeStock("SKU-1004", 1);
                    succeeded.incrementAndGet();
                } catch (InsufficientStockException ignored) {
                    // expected once the stock is exhausted
                }
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(stock, succeeded.get());
        assertEquals(0, service.find("SKU-1004").stock());
    }
}
