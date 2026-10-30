package com.workshop.logproxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.workshop.logproxy.proxy.CallStatus;
import com.workshop.logproxy.proxy.LogEntry;
import com.workshop.logproxy.proxy.LogStore;
import com.workshop.logproxy.proxy.LoggingProxy;
import com.workshop.logproxy.proxy.MicroserviceProxy;

class LoggingProxyTest {

    private final LogStore store = new LogStore(100);

    @Test
    void logsSuccessfulCalls() {
        MicroserviceProxy<String> proxy = new LoggingProxy<>("TestService", (op, params) -> "done:" + op, store);

        String result = proxy.execute("ping", "a");

        assertEquals("done:ping", result);
        LogEntry entry = store.all().get(0);
        assertEquals("TestService", entry.serviceId());
        assertEquals("ping", entry.operation());
        assertEquals(CallStatus.SUCCESS, entry.status());
        assertEquals("done:ping", entry.response());
        assertEquals(List.of("a"), entry.params());
        assertNotNull(entry.timestamp());
        UUID.fromString(entry.requestId());
        assertNull(entry.stackTrace());
    }

    @Test
    void logsFailuresWithStackTraceAndRethrows() {
        MicroserviceProxy<String> proxy = new LoggingProxy<>("TestService", (op, params) -> {
            throw new IllegalStateException("boom");
        }, store);

        assertThrows(IllegalStateException.class, () -> proxy.execute("fail"));

        LogEntry entry = store.all().get(0);
        assertEquals(CallStatus.ERROR, entry.status());
        assertEquals("boom", entry.error());
        assertTrue(entry.stackTrace().contains("IllegalStateException"));
    }

    @Test
    void measuresDuration() {
        MicroserviceProxy<String> proxy = new LoggingProxy<>("TestService", (op, params) -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "ok";
        }, store);

        proxy.execute("slow");

        assertTrue(store.all().get(0).durationMs() >= 40);
    }

    @Test
    void storeDropsOldestEntriesBeyondCapacity() {
        LogStore small = new LogStore(2);
        MicroserviceProxy<String> proxy = new LoggingProxy<>("TestService", (op, params) -> op, small);

        proxy.execute("one");
        proxy.execute("two");
        proxy.execute("three");

        assertEquals(List.of("two", "three"), small.all().stream().map(LogEntry::operation).toList());
    }
}
