package com.taller.proxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Behaviour of the simulated microservices.
 *
 * @param paymentFailureRate probability (0..1) that a payment call fails on purpose
 * @param minLatencyMs       lower bound of the simulated latency
 * @param maxLatencyMs       upper bound of the simulated latency
 */
@ConfigurationProperties(prefix = "app.simulation")
public record SimulationProperties(double paymentFailureRate, long minLatencyMs, long maxLatencyMs) {

    public SimulationProperties {
        if (paymentFailureRate < 0 || paymentFailureRate > 1) {
            throw new IllegalArgumentException("paymentFailureRate must be between 0 and 1");
        }
        if (minLatencyMs < 0 || maxLatencyMs < minLatencyMs) {
            throw new IllegalArgumentException("latency bounds must satisfy 0 <= min <= max");
        }
    }
}
