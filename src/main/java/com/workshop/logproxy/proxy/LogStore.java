package com.workshop.logproxy.proxy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Thread-safe in-memory store that keeps the most recent log entries. */
@Component
public class LogStore {

    private final ConcurrentLinkedDeque<LogEntry> entries = new ConcurrentLinkedDeque<>();
    private final AtomicInteger size = new AtomicInteger();
    private final int capacity;

    public LogStore(@Value("${app.logs.capacity:5000}") int capacity) {
        this.capacity = capacity;
    }

    public void add(LogEntry entry) {
        entries.addLast(entry);
        if (size.incrementAndGet() > capacity && entries.pollFirst() != null) {
            size.decrementAndGet();
        }
    }

    public List<LogEntry> all() {
        return new ArrayList<>(entries);
    }

    /** Returns the entries matching the filters, newest first. */
    public List<LogEntry> find(String serviceId, CallStatus status, Instant from, Instant to) {
        return entries.stream()
                .filter(e -> serviceId == null || e.serviceId().equalsIgnoreCase(serviceId))
                .filter(e -> status == null || e.status() == status)
                .filter(e -> from == null || !e.timestamp().isBefore(from))
                .filter(e -> to == null || !e.timestamp().isAfter(to))
                .sorted(Comparator.comparing(LogEntry::timestamp).reversed())
                .toList();
    }
}
