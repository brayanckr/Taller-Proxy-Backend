package com.workshop.logproxy.metrics;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.workshop.logproxy.ProxyConfig;
import com.workshop.logproxy.proxy.CallStatus;
import com.workshop.logproxy.proxy.LogEntry;
import com.workshop.logproxy.proxy.LogStore;

@Service
public class MetricsService {

    private static final List<String> SERVICES = List.of(ProxyConfig.INVENTORY, ProxyConfig.ORDERS, ProxyConfig.PAYMENTS);

    private final LogStore store;

    public MetricsService(LogStore store) {
        this.store = store;
    }

    /** One summary per service, including services that have not been called yet. */
    public List<ServiceSummary> summary() {
        Map<String, List<LogEntry>> byService = store.all().stream()
                .collect(Collectors.groupingBy(LogEntry::serviceId));
        return SERVICES.stream()
                .map(id -> summarize(id, byService.getOrDefault(id, List.of())))
                .toList();
    }

    public PagedLogs logs(String serviceId, CallStatus status, Instant from, Instant to, int page, int size) {
        List<LogEntry> matches = store.find(serviceId, status, from, to);
        int safeSize = Math.max(1, Math.min(size, 200));
        int safePage = Math.max(0, page);
        int fromIndex = (int) Math.min((long) safePage * safeSize, matches.size());
        int toIndex = Math.min(fromIndex + safeSize, matches.size());
        int totalPages = (int) Math.ceil(matches.size() / (double) safeSize);
        return new PagedLogs(matches.subList(fromIndex, toIndex), safePage, safeSize, matches.size(), totalPages);
    }

    private static ServiceSummary summarize(String serviceId, List<LogEntry> entries) {
        long total = entries.size();
        long errors = entries.stream().filter(e -> e.status() == CallStatus.ERROR).count();
        long successes = total - errors;
        double avg = entries.stream().mapToLong(LogEntry::durationMs).average().orElse(0);
        return new ServiceSummary(serviceId, total, successes, errors,
                percentage(successes, total), percentage(errors, total), round(avg));
    }

    private static double percentage(long part, long total) {
        return total == 0 ? 0 : round(part * 100.0 / total);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record PagedLogs(List<LogEntry> content, int page, int size, long totalElements, int totalPages) {
    }
}
