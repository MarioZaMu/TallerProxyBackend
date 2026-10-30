package com.taller.proxy.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taller.proxy.dto.PageResponse;
import com.taller.proxy.exception.PaymentFailedException;
import com.taller.proxy.model.LogEntry;
import com.taller.proxy.model.LogStatus;
import com.taller.proxy.repository.InMemoryLogRepository;
import com.taller.proxy.repository.LogFilter;
import com.taller.proxy.repository.LogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoggingProxyTest {

    private LogRepository repository;
    private final PayloadSanitizer sanitizer = new JacksonPayloadSanitizer(new ObjectMapper());

    @BeforeEach
    void setUp() {
        repository = new InMemoryLogRepository(100);
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    private LogEntry onlyEntry() {
        PageResponse<LogEntry> page = repository.search(LogFilter.none(), 0, 10);
        assertEquals(1, page.totalElements());
        return page.content().get(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void successfulCallRecordsDurationStatusAndResponse() {
        MicroserviceProxy<Object> target = mock(MicroserviceProxy.class);
        when(target.execute(eq("listProducts"), any())).thenAnswer(invocation -> {
            Thread.sleep(25);
            return Map.of("ok", true);
        });
        LoggingProxy<Object> proxy = new LoggingProxy<>("inventory", target, repository, sanitizer);

        Object result = proxy.execute("listProducts", Map.of("page", 1));

        assertEquals(Map.of("ok", true), result);
        LogEntry entry = onlyEntry();
        assertEquals("inventory", entry.serviceId());
        assertEquals("listProducts", entry.operation());
        assertEquals(LogStatus.SUCCESS, entry.status());
        assertTrue(entry.durationMs() >= 20, "duration should reflect the delegated work");
        assertNotNull(UUID.fromString(entry.requestId()));
        assertTrue(Duration.between(entry.timestamp(), Instant.now()).abs().getSeconds() < 5);
        assertEquals(1, entry.params().get("page").asInt());
        assertTrue(entry.response().get("ok").asBoolean());
        assertNull(entry.errorMessage());
        assertNull(entry.stackTraceSummary());
        assertEquals(entry.requestId(), RequestContext.current().orElseThrow());
        Mockito.verify(target).execute(eq("listProducts"), any());
    }

    @Test
    void failingCallRecordsErrorWithSummarizedStackTraceAndRethrows() {
        PaymentFailedException failure = new PaymentFailedException("gateway down");
        MicroserviceProxy<Object> target = (operation, params) -> {
            throw failure;
        };
        LoggingProxy<Object> proxy = new LoggingProxy<>("payments", target, repository, sanitizer);

        PaymentFailedException thrown = assertThrows(PaymentFailedException.class,
                () -> proxy.execute("processPayment", Map.of("amount", 10)));

        assertSame(failure, thrown);
        LogEntry entry = onlyEntry();
        assertEquals(LogStatus.ERROR, entry.status());
        assertEquals("gateway down", entry.errorMessage());
        assertEquals("PaymentFailedException", entry.errorType());
        assertTrue(entry.stackTraceSummary().startsWith(PaymentFailedException.class.getName() + ": gateway down"));
        long frames = entry.stackTraceSummary().lines().filter(line -> line.trim().startsWith("at ")).count();
        assertTrue(frames <= StackTraceSummarizer.DEFAULT_FRAMES);
        assertNull(entry.response());
    }

    @Test
    void everyCallGetsADifferentRequestId() {
        LoggingProxy<Object> proxy = new LoggingProxy<>("orders", (operation, params) -> "x", repository, sanitizer);
        proxy.execute("a");
        proxy.execute("b");
        var entries = repository.search(LogFilter.none(), 0, 10).content();
        assertEquals(2, entries.size());
        assertNotEquals(entries.get(0).requestId(), entries.get(1).requestId());
    }
}
