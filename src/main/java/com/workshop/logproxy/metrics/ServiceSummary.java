package com.workshop.logproxy.metrics;

/** Aggregated usage metrics of one microservice. Rates are percentages (0-100). */
public record ServiceSummary(
        String serviceId,
        long totalCalls,
        long successCount,
        long errorCount,
        double successRate,
        double errorRate,
        double avgResponseTimeMs) {
}
