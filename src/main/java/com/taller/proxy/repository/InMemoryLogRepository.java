package com.taller.proxy.repository;

import com.taller.proxy.dto.PageResponse;
import com.taller.proxy.model.LogEntry;
import com.taller.proxy.model.LogStatus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Thread-safe, bounded store: the newest entries are kept at the head and the oldest are evicted. */
public class InMemoryLogRepository implements LogRepository {

    private final int maxEntries;
    private final Deque<LogEntry> entries = new ArrayDeque<>();
    private final Map<String, ServiceStats> stats = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public InMemoryLogRepository(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.maxEntries = maxEntries;
    }

    @Override
    public void save(LogEntry entry) {
        lock.writeLock().lock();
        try {
            entries.addFirst(entry);
            while (entries.size() > maxEntries) {
                entries.removeLast();
            }
            boolean success = entry.status() == LogStatus.SUCCESS;
            stats.merge(entry.serviceId(),
                    new ServiceStats(1, success ? 1 : 0, success ? 0 : 1, entry.durationMs()),
                    ServiceStats::plus);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public PageResponse<LogEntry> search(LogFilter filter, int page, int size) {
        List<LogEntry> matching = new ArrayList<>();
        lock.readLock().lock();
        try {
            for (LogEntry entry : entries) {
                if (filter.matches(entry)) {
                    matching.add(entry);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        long offset = (long) page * size;
        List<LogEntry> content = offset >= matching.size()
                ? List.of()
                : List.copyOf(matching.subList((int) offset, (int) Math.min(matching.size(), offset + size)));
        return PageResponse.of(content, page, size, matching.size());
    }

    @Override
    public Map<String, ServiceStats> stats() {
        lock.readLock().lock();
        try {
            return Map.copyOf(stats);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public long count() {
        lock.readLock().lock();
        try {
            return entries.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            entries.clear();
            stats.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
