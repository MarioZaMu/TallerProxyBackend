package com.taller.proxy.repository;

/** Lifetime counters of a service; unlike the bounded log store they are never evicted. */
public record ServiceStats(long total, long success, long error, long totalDurationMs) {

    public static ServiceStats empty() {
        return new ServiceStats(0, 0, 0, 0);
    }

    public ServiceStats plus(ServiceStats other) {
        return new ServiceStats(total + other.total, success + other.success, error + other.error,
                totalDurationMs + other.totalDurationMs);
    }
}
