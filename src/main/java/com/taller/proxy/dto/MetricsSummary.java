package com.taller.proxy.dto;

import java.time.Instant;
import java.util.List;

public record MetricsSummary(Instant generatedAt, List<ServiceMetrics> services, ServiceMetrics global) {
}
