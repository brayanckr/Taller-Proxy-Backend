package com.workshop.logproxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.workshop.logproxy.proxy.LogStore;
import com.workshop.logproxy.proxy.LoggingProxy;
import com.workshop.logproxy.proxy.MicroserviceProxy;
import com.workshop.logproxy.service.InventoryService;
import com.workshop.logproxy.service.OrderService;
import com.workshop.logproxy.service.PaymentService;

/** Wires every real service behind its own LoggingProxy. Controllers only see the proxies. */
@Configuration
public class ProxyConfig implements WebMvcConfigurer {

    public static final String INVENTORY = "InventoryService";
    public static final String ORDERS = "OrderService";
    public static final String PAYMENTS = "PaymentService";

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    MicroserviceProxy<Object> inventoryProxy(LogStore store) {
        return new LoggingProxy<>(INVENTORY, new InventoryService(), store);
    }

    @Bean
    MicroserviceProxy<Object> orderProxy(LogStore store) {
        return new LoggingProxy<>(ORDERS, new OrderService(), store);
    }

    @Bean
    MicroserviceProxy<Object> paymentProxy(LogStore store, @Value("${app.payment.failure-rate}") double failureRate) {
        return new LoggingProxy<>(PAYMENTS, new PaymentService(failureRate), store);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS");
    }
}
