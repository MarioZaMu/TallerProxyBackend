package com.taller.proxy.service;

import com.taller.proxy.config.ProxyRegistry;
import com.taller.proxy.dto.MetricsSummary;
import com.taller.proxy.dto.PageResponse;
import com.taller.proxy.dto.ServiceMetrics;
import com.taller.proxy.exception.InvalidParameterException;
import com.taller.proxy.model.LogEntry;
import com.taller.proxy.model.LogStatus;
import com.taller.proxy.repository.LogFilter;
import com.taller.proxy.repository.LogRepository;
import com.taller.proxy.repository.ServiceStats;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    public static final int MAX_PAGE_SIZE = 200;

    private final LogRepository logRepository;
    private final ProxyRegistry registry;

    public MetricsService(LogRepository logRepository, ProxyRegistry registry) {
        this.logRepository = logRepository;
        this.registry = registry;
    }

    public MetricsSummary summary() {
        Map<String, ServiceStats> stats = logRepository.stats();
        List<ServiceMetrics> services = new ArrayList<>();
        ServiceStats global = ServiceStats.empty();
        for (String serviceId : registry.serviceIds()) {
            ServiceStats serviceStats = stats.getOrDefault(serviceId, ServiceStats.empty());
            services.add(toMetrics(serviceId, serviceStats));
            global = global.plus(serviceStats);
        }
        return new MetricsSummary(Instant.now(), services, toMetrics("global", global));
    }

    public PageResponse<LogEntry> logs(String serviceId, LogStatus status, Instant from, Instant to,
                                       int page, int size) {
        if (page < 0) {
            throw new InvalidParameterException("'page' must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidParameterException("'size' must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (serviceId != null && !registry.contains(serviceId)) {
            throw new InvalidParameterException("Unknown service '" + serviceId + "'. Supported: "
                    + registry.serviceIds());
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidParameterException("'from' must not be after 'to'");
        }
        return logRepository.search(new LogFilter(serviceId, status, from, to), page, size);
    }

    static ServiceMetrics toMetrics(String serviceId, ServiceStats stats) {
        if (stats.total() == 0) {
            return new ServiceMetrics(serviceId, 0, 0, 0, 0, 0, 0);
        }
        double errorRate = round(stats.error() * 100.0 / stats.total());
        double successRate = round(stats.success() * 100.0 / stats.total());
        double average = round((double) stats.totalDurationMs() / stats.total());
        return new ServiceMetrics(serviceId, stats.total(), stats.success(), stats.error(), errorRate, successRate,
                average);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
