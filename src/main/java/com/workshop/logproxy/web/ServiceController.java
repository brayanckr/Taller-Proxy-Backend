package com.workshop.logproxy.web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workshop.logproxy.proxy.MicroserviceProxy;

/** Entry points to the business microservices. Every call goes through its LoggingProxy. */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final MicroserviceProxy<Object> inventory;
    private final MicroserviceProxy<Object> orders;
    private final MicroserviceProxy<Object> payments;

    public ServiceController(
            @Qualifier("inventoryProxy") MicroserviceProxy<Object> inventory,
            @Qualifier("orderProxy") MicroserviceProxy<Object> orders,
            @Qualifier("paymentProxy") MicroserviceProxy<Object> payments) {
        this.inventory = inventory;
        this.orders = orders;
        this.payments = payments;
    }

    @PostMapping("/inventory/{operation}")
    public Object inventory(@PathVariable String operation, @RequestBody(required = false) Map<String, Object> body) {
        return inventory.execute(operation, argsOf(body));
    }

    @PostMapping("/orders/{operation}")
    public Object orders(@PathVariable String operation, @RequestBody(required = false) Map<String, Object> body) {
        return orders.execute(operation, argsOf(body));
    }

    @PostMapping("/payments/{operation}")
    public Object payments(@PathVariable String operation, @RequestBody(required = false) Map<String, Object> body) {
        return payments.execute(operation, argsOf(body));
    }

    private static Map<String, Object> argsOf(Map<String, Object> body) {
        return body == null ? Map.of() : body;
    }
}
