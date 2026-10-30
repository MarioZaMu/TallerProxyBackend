package com.taller.proxy.service;

import com.taller.proxy.config.SimulationProperties;
import com.taller.proxy.exception.UnknownOperationException;
import com.taller.proxy.proxy.MicroserviceProxy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * Template for the real services ("RealSubject"): dispatches named operations to registered handlers
 * and adds a simulated, configurable latency.
 */
public abstract class SimulatedService implements MicroserviceProxy<Object> {

    private final String serviceId;
    private final SimulationProperties properties;
    private final Map<String, Function<OperationParams, Object>> operations = new LinkedHashMap<>();

    protected SimulatedService(String serviceId, SimulationProperties properties) {
        this.serviceId = serviceId;
        this.properties = properties;
    }

    protected void register(String name, Function<OperationParams, Object> handler) {
        operations.put(name, handler);
    }

    @Override
    public final Object execute(String operation, Object... params) {
        Function<OperationParams, Object> handler = operations.get(operation);
        if (handler == null) {
            throw new UnknownOperationException(serviceId, operation, operations.keySet());
        }
        simulateLatency();
        beforeExecute(operation);
        return handler.apply(OperationParams.from(params));
    }

    /** Hook for subclasses that need to inject behaviour (e.g. intentional failures) before an operation. */
    protected void beforeExecute(String operation) {
    }

    protected SimulationProperties properties() {
        return properties;
    }

    private void simulateLatency() {
        long min = properties.minLatencyMs();
        long max = properties.maxLatencyMs();
        if (max <= 0) {
            return;
        }
        long delay = min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
