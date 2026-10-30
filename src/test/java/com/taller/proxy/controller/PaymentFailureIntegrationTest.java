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
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.simulation.payment-failure-rate=1.0",
        "app.simulation.min-latency-ms=0",
        "app.simulation.max-latency-ms=0"})
@AutoConfigureMockMvc
class PaymentFailureIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private LogRepository logRepository;

    @BeforeEach
    void clean() {
        logRepository.clear();
    }

    @Test
    void forcedPaymentFailureReturns502LogsErrorAndRaisesErrorRate() throws Exception {
        mvc.perform(post("/api/services/payments/processPayment").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"ORD-1\",\"amount\":100}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.requestId").exists());

        mvc.perform(get("/api/metrics/logs?service=payments&status=ERROR"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].errorType").value("PaymentFailedException"))
                .andExpect(jsonPath("$.content[0].stackTraceSummary", containsString("PaymentFailedException")))
                .andExpect(jsonPath("$.content[0].params.orderId").value("ORD-1"));

        mvc.perform(get("/api/metrics/summary"))
                .andExpect(jsonPath("$.services[2].serviceId").value("payments"))
                .andExpect(jsonPath("$.services[2].errorRate").value(100.0))
                .andExpect(jsonPath("$.services[2].errorCalls", greaterThan(0)));
    }
}
