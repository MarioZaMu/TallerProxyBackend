package com.taller.proxy.dto;

import java.util.Map;

public record SimulationResult(
        int totalCalls,
        int successCalls,
        int errorCalls,
        long durationMs,
        Map<String, CallCount> byService) {

    public record CallCount(int total, int success, int error) {
    }
}
