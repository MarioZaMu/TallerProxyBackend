package com.taller.proxy.repository;

import com.fasterxml.jackson.databind.node.NullNode;
import com.taller.proxy.dto.PageResponse;
import com.taller.proxy.model.LogEntry;
import com.taller.proxy.model.LogStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryLogRepositoryTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");
    private InMemoryLogRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryLogRepository(5000);
    }

    private static LogEntry entry(int n, String service, LogStatus status) {
        return new LogEntry("req-" + n, service, "op", 10 + n, status, BASE.plusSeconds(n), NullNode.getInstance(),
                null, null, null, null);
    }

    @Test
    void returnsNewestFirstAndPaginates() {
        for (int i = 0; i < 25; i++) {
            repository.save(entry(i, "inventory", LogStatus.SUCCESS));
        }
        PageResponse<LogEntry> first = repository.search(LogFilter.none(), 0, 10);
        assertEquals(25, first.totalElements());
        assertEquals(3, first.totalPages());
        assertEquals("req-24", first.content().get(0).requestId());
        PageResponse<LogEntry> last = repository.search(LogFilter.none(), 2, 10);
        assertEquals(5, last.content().size());
        assertEquals("req-4", last.content().get(0).requestId());
        assertEquals(0, repository.search(LogFilter.none(), 9, 10).content().size());
    }

    @Test
    void filtersByServiceStatusAndDateRange() {
        for (int i = 0; i < 12; i++) {
            repository.save(entry(i, i % 2 == 0 ? "orders" : "payments",
                    i % 3 == 0 ? LogStatus.ERROR : LogStatus.SUCCESS));
        }
        assertEquals(6, repository.search(new LogFilter("orders", null, null, null), 0, 50).totalElements());
        assertEquals(4, repository.search(new LogFilter(null, LogStatus.ERROR, null, null), 0, 50).totalElements());
        assertEquals(2, repository.search(new LogFilter("payments", LogStatus.ERROR, null, null), 0, 50)
                .totalElements());
        assertEquals(4, repository.search(new LogFilter(null, null, BASE.plusSeconds(4), BASE.plusSeconds(7)), 0, 50)
                .totalElements());
        assertEquals(1, repository.search(new LogFilter("orders", LogStatus.ERROR, BASE.plusSeconds(1),
                BASE.plusSeconds(8)), 0, 50).totalElements());
    }

    @Test
    void boundedStoreEvictsOldestButKeepsLifetimeStats() {
        InMemoryLogRepository small = new InMemoryLogRepository(3);
        for (int i = 0; i < 10; i++) {
            small.save(entry(i, "orders", i < 2 ? LogStatus.ERROR : LogStatus.SUCCESS));
        }
        assertEquals(3, small.count());
        assertEquals("req-9", small.search(LogFilter.none(), 0, 10).content().get(0).requestId());
        ServiceStats stats = small.stats().get("orders");
        assertEquals(10, stats.total());
        assertEquals(2, stats.error());
        assertEquals(8, stats.success());
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new InMemoryLogRepository(0));
    }
}
