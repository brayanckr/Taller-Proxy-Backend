package com.workshop.logproxy.service;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Helpers shared by the simulated services to read their input parameters. */
final class Params {

    private Params() {
    }

    /** The first parameter of every call is a map with the operation arguments. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object... params) {
        if (params.length > 0 && params[0] instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    static String requireString(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return value.toString();
    }

    static int requirePositiveInt(Map<String, Object> args, String key) {
        try {
            int value = Integer.parseInt(requireString(args, key));
            if (value <= 0) {
                throw new IllegalArgumentException("Parameter " + key + " must be greater than zero");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter " + key + " must be an integer");
        }
    }

    static double requirePositiveNumber(Map<String, Object> args, String key) {
        try {
            double value = Double.parseDouble(requireString(args, key));
            if (value <= 0) {
                throw new IllegalArgumentException("Parameter " + key + " must be greater than zero");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter " + key + " must be a number");
        }
    }

    /** Simulates network and processing latency. */
    static void simulateLatency(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
