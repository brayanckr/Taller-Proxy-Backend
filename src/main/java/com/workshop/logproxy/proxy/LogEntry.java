package com.workshop.logproxy.proxy;

import java.time.Instant;
import java.util.List;

/**
 * One audited call to a microservice.
 *
 * @param params     input parameters of the call
 * @param response   value returned by the service (null when the call failed)
 * @param error      error message (null when the call succeeded)
 * @param stackTrace summarized stack trace (null when the call succeeded)
 */
public record LogEntry(
        String requestId,
        String serviceId,
        String operation,
        long durationMs,
        CallStatus status,
        Instant timestamp,
        List<Object> params,
        Object response,
        String error,
        String stackTrace) {
}
