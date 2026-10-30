package com.taller.proxy.repository;

import com.taller.proxy.model.LogEntry;
import com.taller.proxy.model.LogStatus;

import java.time.Instant;

/** Optional criteria; {@code null} means "do not filter". {@code from} and {@code to} are inclusive. */
public record LogFilter(String serviceId, LogStatus status, Instant from, Instant to) {

    public static LogFilter none() {
        return new LogFilter(null, null, null, null);
    }

    public boolean matches(LogEntry entry) {
        return (serviceId == null || serviceId.equals(entry.serviceId()))
                && (status == null || status == entry.status())
                && (from == null || !entry.timestamp().isBefore(from))
                && (to == null || !entry.timestamp().isAfter(to));
    }
}
