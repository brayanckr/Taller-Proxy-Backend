package com.workshop.logproxy.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.workshop.logproxy.proxy.MicroserviceProxy;

/** Real inventory microservice. Operations: list, check-stock, reserve. */
public class InventoryService implements MicroserviceProxy<Object> {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of(
            "P-001", 5000,
            "P-002", 5000,
            "P-003", 5000,
            "P-004", 5000));

    @Override
    public Object execute(String operation, Object... params) {
        Params.simulateLatency(20, 120);
        Map<String, Object> args = Params.asMap(params);
        return switch (operation) {
            case "list" -> new LinkedHashMap<>(stock);
            case "check-stock" -> {
                String productId = Params.requireString(args, "productId");
                yield Map.of("productId", productId, "available", availableOf(productId));
            }
            case "reserve" -> reserve(Params.requireString(args, "productId"), Params.requirePositiveInt(args, "quantity"));
            default -> throw new IllegalArgumentException("Unknown inventory operation: " + operation);
        };
    }

    private int availableOf(String productId) {
        Integer available = stock.get(productId);
        if (available == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }
        return available;
    }

    private Object reserve(String productId, int quantity) {
        availableOf(productId);
        int remaining = stock.compute(productId, (id, current) -> {
            if (current < quantity) {
                throw new IllegalStateException("Insufficient stock for " + id + ": requested " + quantity + ", available " + current);
            }
            return current - quantity;
        });
        return Map.of("productId", productId, "reserved", quantity, "remaining", remaining);
    }
}
