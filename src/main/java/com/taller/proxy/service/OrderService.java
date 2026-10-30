package com.taller.proxy.service;

import com.taller.proxy.config.SimulationProperties;
import com.taller.proxy.exception.ConflictException;
import com.taller.proxy.exception.InvalidParameterException;
import com.taller.proxy.exception.ResourceNotFoundException;
import com.taller.proxy.model.Order;
import com.taller.proxy.model.OrderItem;
import com.taller.proxy.model.OrderStatus;
import com.taller.proxy.model.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Orders microservice. Creating an order checks and reserves stock in the inventory service. */
@Service
public class OrderService extends SimulatedService {

    public static final String SERVICE_ID = "orders";
    private static final int LIST_LIMIT = 100;

    private final InventoryService inventory;
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1000);

    public OrderService(SimulationProperties properties, InventoryService inventory) {
        super(SERVICE_ID, properties);
        this.inventory = inventory;
        register("createOrder", p -> createOrder(p.requireString("customerId"), p.requireObjectList("items")));
        register("getOrder", p -> getOrder(p.requireString("orderId")));
        register("listOrders", p -> listOrders(p.optionalString("status", null)));
        register("cancelOrder", p -> cancelOrder(p.requireString("orderId")));
    }

    public Order createOrder(String customerId, List<OperationParams> itemParams) {
        List<OrderItem> items = new ArrayList<>();
        List<OrderItem> reserved = new ArrayList<>();
        try {
            for (OperationParams itemParam : itemParams) {
                String sku = itemParam.requireString("sku");
                int quantity = itemParam.requirePositiveInt("quantity");
                if (!inventory.checkStock(sku, quantity).available()) {
                    int stock = inventory.find(sku).stock();
                    throw new com.taller.proxy.exception.InsufficientStockException(sku, stock, quantity);
                }
                Product product = inventory.reserve(sku, quantity);
                OrderItem item = new OrderItem(sku, product.name(), quantity, product.price());
                reserved.add(item);
                items.add(item);
            }
        } catch (RuntimeException e) {
            reserved.forEach(item -> inventory.release(item.sku(), item.quantity()));
            throw e;
        }
        BigDecimal total = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant now = Instant.now();
        Order order = new Order("ORD-" + sequence.incrementAndGet(), customerId, List.copyOf(items), total,
                OrderStatus.CREATED, now, now);
        orders.put(order.orderId(), order);
        return order;
    }

    public Order getOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        return order;
    }

    public List<Order> listOrders(String status) {
        OrderStatus filter = null;
        if (status != null) {
            try {
                filter = OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterException("'status' must be one of CREATED, CANCELLED");
            }
        }
        OrderStatus wanted = filter;
        return orders.values().stream()
                .filter(order -> wanted == null || order.status() == wanted)
                .sorted(Comparator.comparing(Order::createdAt).reversed())
                .limit(LIST_LIMIT)
                .toList();
    }

    public Order cancelOrder(String orderId) {
        Order cancelled = orders.compute(orderId, (id, current) -> {
            if (current == null) {
                throw new ResourceNotFoundException("Order not found: " + id);
            }
            if (current.status() == OrderStatus.CANCELLED) {
                throw new ConflictException("Order already cancelled: " + id);
            }
            return current.withStatus(OrderStatus.CANCELLED);
        });
        cancelled.items().forEach(item -> inventory.release(item.sku(), item.quantity()));
        return cancelled;
    }
}
