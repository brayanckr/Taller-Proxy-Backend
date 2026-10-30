package com.workshop.logproxy.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.workshop.logproxy.proxy.MicroserviceProxy;

/** Real orders microservice. Operations: list, get, create. */
public class OrderService implements MicroserviceProxy<Object> {

    private final Map<String, Map<String, Object>> orders = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(1000);

    @Override
    public Object execute(String operation, Object... params) {
        Params.simulateLatency(30, 180);
        Map<String, Object> args = Params.asMap(params);
        return switch (operation) {
            case "list" -> new ArrayList<>(orders.values());
            case "get" -> get(Params.requireString(args, "orderId"));
            case "create" -> create(
                    Params.requireString(args, "customer"),
                    Params.requireString(args, "productId"),
                    Params.requirePositiveInt(args, "quantity"));
            default -> throw new IllegalArgumentException("Unknown orders operation: " + operation);
        };
    }

    private Map<String, Object> get(String orderId) {
        Map<String, Object> order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        return order;
    }

    private Map<String, Object> create(String customer, String productId, int quantity) {
        String orderId = "O-" + sequence.incrementAndGet();
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderId", orderId);
        order.put("customer", customer);
        order.put("productId", productId);
        order.put("quantity", quantity);
        order.put("status", "CREATED");
        orders.put(orderId, order);
        return order;
    }
}
