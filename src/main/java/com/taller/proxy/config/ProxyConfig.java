package com.taller.proxy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taller.proxy.proxy.JacksonPayloadSanitizer;
import com.taller.proxy.proxy.LoggingProxy;
import com.taller.proxy.proxy.MicroserviceProxy;
import com.taller.proxy.proxy.PayloadSanitizer;
import com.taller.proxy.repository.InMemoryLogRepository;
import com.taller.proxy.repository.LogRepository;
import com.taller.proxy.service.InventoryService;
import com.taller.proxy.service.OrderService;
import com.taller.proxy.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/** Wires every real service behind its own LoggingProxy. */
@Configuration
public class ProxyConfig {

    @Bean
    public LogRepository logRepository(@Value("${app.logs.max-entries:5000}") int maxEntries) {
        return new InMemoryLogRepository(maxEntries);
    }

    @Bean
    public PayloadSanitizer payloadSanitizer(ObjectMapper objectMapper) {
        return new JacksonPayloadSanitizer(objectMapper);
    }

    @Bean
    public ProxyRegistry proxyRegistry(InventoryService inventory, OrderService orders, PaymentService payments,
                                       LogRepository logRepository, PayloadSanitizer sanitizer) {
        Map<String, MicroserviceProxy<Object>> proxies = new LinkedHashMap<>();
        proxies.put(InventoryService.SERVICE_ID,
                new LoggingProxy<>(InventoryService.SERVICE_ID, inventory, logRepository, sanitizer));
        proxies.put(OrderService.SERVICE_ID,
                new LoggingProxy<>(OrderService.SERVICE_ID, orders, logRepository, sanitizer));
        proxies.put(PaymentService.SERVICE_ID,
                new LoggingProxy<>(PaymentService.SERVICE_ID, payments, logRepository, sanitizer));
        return new ProxyRegistry(proxies);
    }
}
