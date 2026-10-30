package com.taller.proxy.controller;

import com.taller.proxy.dto.MetricsSummary;
import com.taller.proxy.dto.PageResponse;
import com.taller.proxy.dto.SimulationResult;
import com.taller.proxy.model.LogEntry;
import com.taller.proxy.model.LogStatus;
import com.taller.proxy.service.LoadSimulationService;
import com.taller.proxy.service.MetricsService;
import com.taller.proxy.util.IsoDateParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;
    private final LoadSimulationService simulationService;

    public MetricsController(MetricsService metricsService, LoadSimulationService simulationService) {
        this.metricsService = metricsService;
        this.simulationService = simulationService;
    }

    @GetMapping("/summary")
    public MetricsSummary summary() {
        return metricsService.summary();
    }

    @GetMapping("/logs")
    public PageResponse<LogEntry> logs(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) LogStatus status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return metricsService.logs(blankToNull(service), status,
                IsoDateParser.parse("from", from, false), IsoDateParser.parse("to", to, true), page, size);
    }

    @PostMapping("/simulate-load")
    public SimulationResult simulateLoad() {
        return simulationService.run();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
