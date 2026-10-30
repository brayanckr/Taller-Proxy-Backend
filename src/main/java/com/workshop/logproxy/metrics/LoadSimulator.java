package com.workshop.logproxy.metrics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.workshop.logproxy.proxy.MicroserviceProxy;

/** Generates random calls through the proxies to populate the logs and metrics. */
@Component
public class LoadSimulator {

    private static final List<String> PRODUCTS = List.of("P-001", "P-002", "P-003", "P-004");

    private static final double BAD_REQUEST_RATE = 0.03;

    private final List<Supplier<Object>> calls;
    private final List<Supplier<Object>> badCalls;

    public LoadSimulator(
            @Qualifier("inventoryProxy") MicroserviceProxy<Object> inventory,
            @Qualifier("orderProxy") MicroserviceProxy<Object> orders,
            @Qualifier("paymentProxy") MicroserviceProxy<Object> payments) {
        this.badCalls = List.of(
                () -> inventory.execute("check-stock", Map.of("productId", "P-999")),
                () -> orders.execute("get", Map.of("orderId", "O-0")));
        this.calls = List.of(
                () -> inventory.execute("check-stock", Map.of("productId", randomProduct())),
                () -> inventory.execute("reserve", Map.of("productId", randomProduct(), "quantity", 1)),
                () -> inventory.execute("list", Map.of()),
                () -> orders.execute("create", Map.of("customer", "customer-" + randomQuantity(),
                        "productId", randomProduct(), "quantity", randomQuantity())),
                () -> orders.execute("list", Map.of()),
                () -> payments.execute("charge", Map.of("orderId", "O-" + (1000 + randomQuantity()), "amount", randomAmount())),
                () -> payments.execute("charge", Map.of("orderId", "O-" + (1000 + randomQuantity()), "amount", randomAmount())),
                () -> payments.execute("refund", Map.of("orderId", "O-" + (1000 + randomQuantity()), "amount", randomAmount())));
    }

    /** Executes the given number of random calls and returns how many succeeded and failed. */
    public SimulationResult run(int count) {
        int errors = 0;
        for (int i = 0; i < count; i++) {
            try {
                List<Supplier<Object>> pool = ThreadLocalRandom.current().nextDouble() < BAD_REQUEST_RATE ? badCalls : calls;
                pool.get(ThreadLocalRandom.current().nextInt(pool.size())).get();
            } catch (RuntimeException e) {
                errors++;
            }
        }
        return new SimulationResult(count, count - errors, errors);
    }

    private static String randomProduct() {
        return PRODUCTS.get(ThreadLocalRandom.current().nextInt(PRODUCTS.size()));
    }

    private static int randomQuantity() {
        return ThreadLocalRandom.current().nextInt(1, 4);
    }

    private static double randomAmount() {
        return Math.round(ThreadLocalRandom.current().nextDouble(10, 500) * 100.0) / 100.0;
    }

    public record SimulationResult(int executed, int success, int errors) {
    }
}
