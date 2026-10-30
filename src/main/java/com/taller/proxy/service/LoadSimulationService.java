package com.taller.proxy.service;

import com.taller.proxy.config.ProxyRegistry;
import com.taller.proxy.dto.SimulationResult;
import com.taller.proxy.dto.SimulationResult.CallCount;
import com.taller.proxy.model.Order;
import com.taller.proxy.model.OrderStatus;
import com.taller.proxy.model.Payment;
import com.taller.proxy.model.PaymentMethod;
import com.taller.proxy.model.PaymentStatus;
import com.taller.proxy.model.Product;
import com.taller.proxy.proxy.RequestContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/** Generates random, valid traffic through the logging proxies to populate the logs. */
@Service
public class LoadSimulationService {

    public static final int DEFAULT_CALLS = 50;
    private static final int WORKERS = 10;

    private final ProxyRegistry registry;
    private final InventoryService inventory;
    private final List<String> knownOrderIds = new CopyOnWriteArrayList<>();
    private final List<String> knownPaymentIds = new CopyOnWriteArrayList<>();

    public LoadSimulationService(ProxyRegistry registry, InventoryService inventory) {
        this.registry = registry;
        this.inventory = inventory;
    }

    public SimulationResult run() {
        return run(DEFAULT_CALLS);
    }

    public SimulationResult run(int calls) {
        long start = System.nanoTime();
        Map<String, AtomicInteger[]> counters = new LinkedHashMap<>();
        registry.serviceIds().forEach(id -> counters.put(id, new AtomicInteger[]{new AtomicInteger(),
                new AtomicInteger(), new AtomicInteger()}));
        List<String> skus = inventory.listProducts().stream().map(Product::sku).toList();

        ExecutorService pool = Executors.newFixedThreadPool(WORKERS);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < calls; i++) {
                futures.add(pool.submit(() -> singleCall(skus, counters)));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("Simulation worker failed", e.getCause());
        } finally {
            pool.shutdown();
        }

        int success = 0;
        int error = 0;
        Map<String, CallCount> byService = new LinkedHashMap<>();
        for (Map.Entry<String, AtomicInteger[]> entry : counters.entrySet()) {
            int ok = entry.getValue()[1].get();
            int ko = entry.getValue()[2].get();
            byService.put(entry.getKey(), new CallCount(ok + ko, ok, ko));
            success += ok;
            error += ko;
        }
        return new SimulationResult(success + error, success, error, (System.nanoTime() - start) / 1_000_000,
                byService);
    }

    private void singleCall(List<String> skus, Map<String, AtomicInteger[]> counters) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int roll = random.nextInt(100);
        String serviceId = roll < 30 ? InventoryService.SERVICE_ID
                : roll < 65 ? OrderService.SERVICE_ID : PaymentService.SERVICE_ID;
        Call call = switch (serviceId) {
            case InventoryService.SERVICE_ID -> inventoryCall(skus, random);
            case OrderService.SERVICE_ID -> orderCall(skus, random);
            default -> paymentCall(random);
        };
        AtomicInteger[] counter = counters.get(serviceId);
        try {
            Object result = registry.require(serviceId).execute(call.operation(), call.params());
            remember(result);
            counter[1].incrementAndGet();
        } catch (RuntimeException e) {
            counter[2].incrementAndGet();
        } finally {
            RequestContext.clear();
        }
    }

    private Call inventoryCall(List<String> skus, ThreadLocalRandom random) {
        String sku = skus.get(random.nextInt(skus.size()));
        int roll = random.nextInt(10);
        if (roll < 2) {
            return new Call("listProducts", Map.of());
        } else if (roll < 6) {
            return new Call("checkStock", Map.of("sku", sku, "quantity", random.nextInt(1, 6)));
        } else if (roll < 9) {
            return new Call("addStock", Map.of("sku", sku, "quantity", random.nextInt(5, 21)));
        }
        return new Call("removeStock", Map.of("sku", sku, "quantity", random.nextInt(1, 3)));
    }

    private Call orderCall(List<String> skus, ThreadLocalRandom random) {
        int roll = random.nextInt(10);
        if (roll < 4 || knownOrderIds.isEmpty()) {
            return createOrderCall(skus, random);
        } else if (roll < 6) {
            return new Call("getOrder", Map.of("orderId", randomOf(knownOrderIds, random)));
        } else if (roll < 8) {
            return new Call("listOrders", Map.of());
        }
        String orderId = randomOf(knownOrderIds, random);
        // Only the thread that actually removes the id may cancel it, so no order is cancelled twice.
        if (!knownOrderIds.remove(orderId)) {
            return new Call("listOrders", Map.of());
        }
        return new Call("cancelOrder", Map.of("orderId", orderId));
    }

    private Call createOrderCall(List<String> skus, ThreadLocalRandom random) {
        int lines = random.nextInt(1, 4);
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < lines; i++) {
            final String sku = skus.get((random.nextInt(skus.size()) + i) % skus.size());
            items.add(Map.of("sku", sku, "quantity", random.nextInt(1, 3)));
        }
        return new Call("createOrder", Map.of("customerId", "CUST-" + random.nextInt(100, 1000), "items", items));
    }

    private Call paymentCall(ThreadLocalRandom random) {
        int roll = random.nextInt(10);
        if (roll < 5 || knownPaymentIds.isEmpty()) {
            PaymentMethod[] methods = PaymentMethod.values();
            String orderId = knownOrderIds.isEmpty() ? "ORD-1001" : randomOf(knownOrderIds, random);
            return new Call("processPayment", Map.of("orderId", orderId,
                    "amount", random.nextInt(50, 5000) * 1000,
                    "method", methods[random.nextInt(methods.length)].name()));
        } else if (roll < 9) {
            return new Call("getPaymentStatus", Map.of("paymentId", randomOf(knownPaymentIds, random)));
        }
        String paymentId = randomOf(knownPaymentIds, random);
        if (!knownPaymentIds.remove(paymentId)) {
            return new Call("getPaymentStatus", Map.of("paymentId", paymentId));
        }
        return new Call("refundPayment", Map.of("paymentId", paymentId));
    }

    private void remember(Object result) {
        if (result instanceof Order order && order.status() == OrderStatus.CREATED
                && !knownOrderIds.contains(order.orderId())) {
            knownOrderIds.add(order.orderId());
        } else if (result instanceof Payment payment && payment.status() == PaymentStatus.COMPLETED
                && !knownPaymentIds.contains(payment.paymentId())) {
            knownPaymentIds.add(payment.paymentId());
        }
    }

    private static String randomOf(List<String> ids, ThreadLocalRandom random) {
        try {
            return ids.get(random.nextInt(ids.size()));
        } catch (IndexOutOfBoundsException e) {
            return "UNKNOWN";
        }
    }

    private record Call(String operation, Map<String, Object> params) {
    }
}
