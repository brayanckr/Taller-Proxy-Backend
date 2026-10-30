package com.workshop.logproxy.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.workshop.logproxy.proxy.MicroserviceProxy;

/** Real payments microservice. Operations: charge, refund. Fails intentionally at a configurable rate. */
public class PaymentService implements MicroserviceProxy<Object> {

    private final double failureRate;

    public PaymentService(double failureRate) {
        this.failureRate = failureRate;
    }

    @Override
    public Object execute(String operation, Object... params) {
        Params.simulateLatency(50, 250);
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new PaymentGatewayException("Payment gateway failed while processing '" + operation + "'");
        }
        Map<String, Object> args = Params.asMap(params);
        return switch (operation) {
            case "charge" -> {
                String orderId = Params.requireString(args, "orderId");
                double amount = Params.requirePositiveNumber(args, "amount");
                yield receipt("CHARGED", orderId, amount);
            }
            case "refund" -> {
                String orderId = Params.requireString(args, "orderId");
                double amount = Params.requirePositiveNumber(args, "amount");
                yield receipt("REFUNDED", orderId, amount);
            }
            default -> throw new IllegalArgumentException("Unknown payments operation: " + operation);
        };
    }

    private Map<String, Object> receipt(String status, String orderId, double amount) {
        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("paymentId", "PAY-" + UUID.randomUUID().toString().substring(0, 8));
        receipt.put("orderId", orderId);
        receipt.put("amount", amount);
        receipt.put("status", status);
        return receipt;
    }
}
