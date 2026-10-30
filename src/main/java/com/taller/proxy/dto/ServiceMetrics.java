package com.taller.proxy.dto;

/** Rates are expressed as percentages in the range 0-100; the average is in milliseconds. */
public record ServiceMetrics(
        String serviceId,
        long totalCalls,
        long successCalls,
        long errorCalls,
        double errorRate,
        double successRate,
        double avgResponseTimeMs) {
}
