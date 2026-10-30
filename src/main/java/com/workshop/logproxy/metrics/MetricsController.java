package com.workshop.logproxy.metrics;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workshop.logproxy.proxy.CallStatus;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private static final int SIMULATED_CALLS = 50;

    private final MetricsService metrics;
    private final LoadSimulator simulator;

    public MetricsController(MetricsService metrics, LoadSimulator simulator) {
        this.metrics = metrics;
        this.simulator = simulator;
    }

    @GetMapping("/summary")
    public List<ServiceSummary> summary() {
        return metrics.summary();
    }

    @GetMapping("/logs")
    public MetricsService.PagedLogs logs(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) CallStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return metrics.logs(service, status, from, to, page, size);
    }

    @PostMapping("/simulate-load")
    public LoadSimulator.SimulationResult simulateLoad() {
        return simulator.run(SIMULATED_CALLS);
    }
}
