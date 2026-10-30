package com.taller.proxy.service;

import com.taller.proxy.config.SimulationProperties;
import com.taller.proxy.exception.ConflictException;
import com.taller.proxy.exception.InsufficientStockException;
import com.taller.proxy.exception.InvalidParameterException;
import com.taller.proxy.exception.ResourceNotFoundException;
import com.taller.proxy.model.Order;
import com.taller.proxy.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderServiceTest {

    private InventoryService inventory;
    private OrderService orders;

    @BeforeEach
    void setUp() {
        SimulationProperties props = new SimulationProperties(0, 0, 0);
        inventory = new InventoryService(props);
        orders = new OrderService(props, inventory);
    }

    private Order create(String sku, int quantity) {
        return (Order) orders.execute("createOrder",
                Map.of("customerId", "C-1", "items", List.of(Map.of("sku", sku, "quantity", quantity))));
    }

    @Test
    void createOrderReservesStockAndComputesTotal() {
        int before = inventory.find("SKU-1002").stock();
        Order order = create("SKU-1002", 3);
        assertEquals(OrderStatus.CREATED, order.status());
        assertEquals(new BigDecimal("267000"), order.total());
        assertEquals(before - 3, inventory.find("SKU-1002").stock());
        assertEquals(order, orders.execute("getOrder", Map.of("orderId", order.orderId())));
    }

    @Test
    void createOrderRollsBackReservationsWhenALaterItemFails() {
        int before = inventory.find("SKU-1002").stock();
        assertThrows(InsufficientStockException.class, () -> orders.execute("createOrder", Map.of("customerId", "C-1",
                "items", List.of(Map.of("sku", "SKU-1002", "quantity", 2),
                        Map.of("sku", "SKU-1001", "quantity", 99999)))));
        assertEquals(before, inventory.find("SKU-1002").stock());
    }

    @Test
    void cancelOrderReleasesStockAndCannotBeRepeated() {
        int before = inventory.find("SKU-1003").stock();
        Order order = create("SKU-1003", 5);
        Order cancelled = (Order) orders.execute("cancelOrder", Map.of("orderId", order.orderId()));
        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertEquals(before, inventory.find("SKU-1003").stock());
        assertThrows(ConflictException.class, () -> orders.execute("cancelOrder", Map.of("orderId", order.orderId())));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listOrdersSupportsStatusFilter() {
        Order first = create("SKU-1002", 1);
        create("SKU-1002", 1);
        orders.execute("cancelOrder", Map.of("orderId", first.orderId()));
        assertEquals(2, ((List<Order>) orders.execute("listOrders", Map.of())).size());
        assertEquals(1, ((List<Order>) orders.execute("listOrders", Map.of("status", "CANCELLED"))).size());
        assertThrows(InvalidParameterException.class, () -> orders.execute("listOrders", Map.of("status", "??")));
    }

    @Test
    void reportsMissingResourcesAndBadInput() {
        assertThrows(ResourceNotFoundException.class, () -> orders.execute("getOrder", Map.of("orderId", "ORD-0")));
        assertThrows(InvalidParameterException.class,
                () -> orders.execute("createOrder", Map.of("customerId", "C-1")));
        assertThrows(ResourceNotFoundException.class, () -> orders.execute("createOrder",
                Map.of("customerId", "C-1", "items", List.of(Map.of("sku", "NOPE", "quantity", 1)))));
    }
}
