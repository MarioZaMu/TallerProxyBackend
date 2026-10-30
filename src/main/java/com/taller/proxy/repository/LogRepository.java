package com.taller.proxy.repository;

import com.taller.proxy.dto.PageResponse;
import com.taller.proxy.model.LogEntry;

import java.util.Map;

public interface LogRepository {

    void save(LogEntry entry);

    /** Returns the matching entries, newest first, for the requested zero-based page. */
    PageResponse<LogEntry> search(LogFilter filter, int page, int size);

    /** Lifetime counters keyed by serviceId. */
    Map<String, ServiceStats> stats();

    /** Number of entries currently retained. */
    long count();

    void clear();
}
