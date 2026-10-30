package com.workshop.logproxy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.payment.failure-rate=0")
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void callsGoThroughTheProxyAndShowUpInTheLogsAndSummary() throws Exception {
        mvc.perform(post("/api/services/inventory/check-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"P-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").isNumber());

        mvc.perform(get("/api/metrics/logs").param("service", "InventoryService").param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].operation").value("check-stock"))
                .andExpect(jsonPath("$.content[0].requestId").isNotEmpty());

        mvc.perform(get("/api/metrics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].serviceId").value("InventoryService"));
    }

    @Test
    void failedCallsAreLoggedAsErrors() throws Exception {
        mvc.perform(post("/api/services/orders/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"missing\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/metrics/logs").param("service", "OrderService").param("status", "ERROR"))
                .andExpect(jsonPath("$.content[0].error").value("Order not found: missing"))
                .andExpect(jsonPath("$.content[0].stackTrace").isNotEmpty());
    }

    @Test
    void simulateLoadGeneratesFiftyCalls() throws Exception {
        mvc.perform(post("/api/metrics/simulate-load"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executed").value(50));
    }
}
