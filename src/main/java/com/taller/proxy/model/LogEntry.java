package com.taller.proxy.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/** One audited call to a microservice. {@code params} and {@code response} are JSON snapshots. */
public record LogEntry(
        String requestId,
        String serviceId,
        String operation,
        long durationMs,
        LogStatus status,
        Instant timestamp,
        JsonNode params,
        JsonNode response,
        String errorType,
        String errorMessage,
        String stackTraceSummary) {
}
