package com.taller.proxy.service;

import com.fasterxml.jackson.databind.node.NullNode;
import com.taller.proxy.config.ProxyRegistry;
import com.taller.proxy.dto.MetricsSummary;
import com.taller.proxy.dto.ServiceMetrics;
import com.taller.proxy.exception.InvalidParameterException;
import com.taller.proxy.model.LogEntry;
import com.taller.proxy.model.LogStatus;
import com.taller.proxy.proxy.MicroserviceProxy;
import com.taller.proxy.repository.InMemoryLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricsServiceTest {

    private InMemoryLogRepository repository;
    private MetricsService metrics;

    @BeforeEach
    void setUp() {
        repository = new InMemoryLogRepository(100);
        Map<String, MicroserviceProxy<Object>> proxies = new LinkedHashMap<>();
        for (String id : new String[]{"inventory", "orders", "payments"}) {
            proxies.put(id, (op, params) -> null);
        }
        metrics = new MetricsService(repository, new ProxyRegistry(proxies));
    }

    private void log(String service, LogStatus status, long durationMs) {
        repository.save(new LogEntry("r", service, "op", durationMs, status, Instant.now(), NullNode.getInstance(),
                null, null, null, null));
    }

    @Test
    void computesPerServiceAndGlobalMetrics() {
        log("payments", LogStatus.SUCCESS, 100);
        log("payments", LogStatus.SUCCESS, 200);
        log("payments", LogStatus.SUCCESS, 300);
        log("payments", LogStatus.ERROR, 400);
        log("orders", LogStatus.SUCCESS, 50);

        MetricsSummary summary = metrics.summary();
        ServiceMetrics payments = summary.services().stream().filter(s -> s.serviceId().equals("payments"))
                .findFirst().orElseThrow();
        assertEquals(4, payments.totalCalls());
        assertEquals(3, payments.successCalls());
        assertEquals(1, payments.errorCalls());
        assertEquals(25.0, payments.errorRate());
        assertEquals(75.0, payments.successRate());
        assertEquals(250.0, payments.avgResponseTimeMs());

        assertEquals(3, summary.services().size());
        ServiceMetrics inventory = summary.services().get(0);
        assertEquals(0, inventory.totalCalls());
        assertEquals(0.0, inventory.errorRate());

        assertEquals(5, summary.global().totalCalls());
        assertEquals(20.0, summary.global().errorRate());
        assertEquals(210.0, summary.global().avgResponseTimeMs());
    }

    @Test
    void validatesLogQueries() {
        assertThrows(InvalidParameterException.class, () -> metrics.logs("ghost", null, null, null, 0, 20));
        assertThrows(InvalidParameterException.class, () -> metrics.logs(null, null, null, null, -1, 20));
        assertThrows(InvalidParameterException.class, () -> metrics.logs(null, null, null, null, 0, 0));
        assertThrows(InvalidParameterException.class, () -> metrics.logs(null, null, null, null, 0, 5000));
        Instant now = Instant.now();
        assertThrows(InvalidParameterException.class,
                () -> metrics.logs(null, null, now, now.minusSeconds(5), 0, 20));
    }
}
