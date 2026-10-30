package com.taller.proxy.controller;

import com.taller.proxy.repository.LogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.simulation.payment-failure-rate=0",
        "app.simulation.min-latency-ms=0",
        "app.simulation.max-latency-ms=0"})
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private LogRepository logRepository;

    @BeforeEach
    void clean() {
        logRepository.clear();
    }

    @Test
    void inventoryEndpointReturnsResultAndRequestId() throws Exception {
        mvc.perform(post("/api/services/inventory/checkStock").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1001\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId", matchesPattern(UUID_PATTERN)))
                .andExpect(jsonPath("$.serviceId").value("inventory"))
                .andExpect(jsonPath("$.result.available").value(true));
    }

    @Test
    void ordersAndPaymentsEndpointsWork() throws Exception {
        mvc.perform(post("/api/services/orders/createOrder").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C-9\",\"items\":[{\"sku\":\"SKU-1002\",\"quantity\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.orderId").exists())
                .andExpect(jsonPath("$.result.status").value("CREATED"));
        mvc.perform(post("/api/services/payments/processPayment").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"ORD-1001\",\"amount\":178000,\"method\":\"CARD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("COMPLETED"));
        mvc.perform(get("/api/metrics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.global.totalCalls").value(2));
    }

    @Test
    void unknownOperationReturns404AndIsLoggedAsError() throws Exception {
        mvc.perform(post("/api/services/inventory/explode").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("explode")))
                .andExpect(jsonPath("$.requestId", matchesPattern(UUID_PATTERN)))
                .andExpect(jsonPath("$.timestamp").exists());
        mvc.perform(get("/api/metrics/logs?status=ERROR"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].operation").value("explode"))
                .andExpect(jsonPath("$.content[0].stackTraceSummary", containsString("UnknownOperationException")));
    }

    @Test
    void unknownServiceAndInvalidBodyReturnJsonErrors() throws Exception {
        mvc.perform(post("/api/services/ghost/list").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("ghost")));
        mvc.perform(post("/api/services/inventory/listProducts").contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/api/services/inventory/addStock").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logsAreFilteredAndPaginated() throws Exception {
        for (int i = 0; i < 7; i++) {
            mvc.perform(post("/api/services/inventory/listProducts").contentType(MediaType.APPLICATION_JSON)
                    .content("{}"));
        }
        mvc.perform(post("/api/services/inventory/nope").contentType(MediaType.APPLICATION_JSON).content("{}"));
        mvc.perform(post("/api/services/orders/getOrder").contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"x\"}"));

        mvc.perform(get("/api/metrics/logs"))
                .andExpect(jsonPath("$.totalElements").value(9))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.page").value(0));
        mvc.perform(get("/api/metrics/logs?service=inventory&page=1&size=5"))
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(3)));
        mvc.perform(get("/api/metrics/logs?service=inventory&status=ERROR"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/metrics/logs?from=2999-01-01"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/metrics/logs?from=2000-01-01&to=2999-01-01T00:00:00Z&status=SUCCESS"))
                .andExpect(jsonPath("$.totalElements").value(7));
        mvc.perform(get("/api/metrics/logs?status=BROKEN")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/metrics/logs?from=yesterday")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/metrics/logs?size=0")).andExpect(status().isBadRequest());
    }

    @Test
    void simulateLoadProducesFiftyLogEntries() throws Exception {
        mvc.perform(post("/api/metrics/simulate-load"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalls").value(50))
                .andExpect(jsonPath("$.successCalls").isNumber())
                .andExpect(jsonPath("$.errorCalls").isNumber());
        mvc.perform(get("/api/metrics/logs?size=200"))
                .andExpect(jsonPath("$.totalElements").value(50));
        mvc.perform(get("/api/metrics/summary"))
                .andExpect(jsonPath("$.global.totalCalls").value(50))
                .andExpect(jsonPath("$.services", hasSize(3)));
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void corsAllowsConfiguredOrigins() throws Exception {
        mvc.perform(options("/api/metrics/summary")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/metrics/summary")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
