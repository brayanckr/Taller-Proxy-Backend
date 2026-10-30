package com.workshop.logproxy.proxy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy that wraps any microservice: it records the start of every call, then its duration and
 * result, and on failure the error with its stack trace.
 */
public class LoggingProxy<T> implements MicroserviceProxy<T> {

    private static final Logger log = LoggerFactory.getLogger(LoggingProxy.class);
    private static final int MAX_STACK_TRACE_LENGTH = 2000;

    private final String serviceId;
    private final MicroserviceProxy<T> target;
    private final LogStore store;

    public LoggingProxy(String serviceId, MicroserviceProxy<T> target, LogStore store) {
        this.serviceId = serviceId;
        this.target = target;
        this.store = store;
    }

    @Override
    public T execute(String operation, Object... params) {
        String requestId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        log.info("[{}] START {}.{} params={}", requestId, serviceId, operation, Arrays.toString(params));
        long startNanos = System.nanoTime();

        try {
            T result = target.execute(operation, params);
            long durationMs = elapsedMs(startNanos);
            store.add(new LogEntry(requestId, serviceId, operation, durationMs, CallStatus.SUCCESS, timestamp,
                    Arrays.asList(params), result, null, null));
            log.info("[{}] END {}.{} status=SUCCESS durationMs={}", requestId, serviceId, operation, durationMs);
            return result;
        } catch (RuntimeException e) {
            long durationMs = elapsedMs(startNanos);
            store.add(new LogEntry(requestId, serviceId, operation, durationMs, CallStatus.ERROR, timestamp,
                    Arrays.asList(params), null, e.getMessage(), stackTraceOf(e)));
            log.error("[{}] END {}.{} status=ERROR durationMs={}", requestId, serviceId, operation, durationMs, e);
            throw e;
        }
    }

    private static long elapsedMs(long startNanos) {
        return Math.round((System.nanoTime() - startNanos) / 1_000_000.0);
    }

    private static String stackTraceOf(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        String trace = writer.toString();
        return trace.length() > MAX_STACK_TRACE_LENGTH ? trace.substring(0, MAX_STACK_TRACE_LENGTH) + "..." : trace;
    }
}
