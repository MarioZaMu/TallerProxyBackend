package com.taller.proxy.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.taller.proxy.model.LogEntry;
import com.taller.proxy.model.LogStatus;
import com.taller.proxy.repository.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

/**
 * Generic logging/auditing proxy (Proxy pattern "Proxy"). It implements the same interface as the
 * real service, holds a reference to it and adds behaviour around the delegation: it records the
 * start of the call, its duration and result, and, on failure, a summarized stack trace. The
 * original exception is always propagated so callers can map it to a proper HTTP error.
 */
public class LoggingProxy<T> implements MicroserviceProxy<T> {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingProxy.class);

    private final String serviceId;
    private final MicroserviceProxy<T> target;
    private final LogRepository logRepository;
    private final PayloadSanitizer sanitizer;

    public LoggingProxy(String serviceId, MicroserviceProxy<T> target, LogRepository logRepository,
                        PayloadSanitizer sanitizer) {
        this.serviceId = serviceId;
        this.target = target;
        this.logRepository = logRepository;
        this.sanitizer = sanitizer;
    }

    @Override
    public T execute(String operation, Object... params) {
        String requestId = UUID.randomUUID().toString();
        RequestContext.set(requestId);
        JsonNode paramsSnapshot = sanitizer.sanitize(params != null && params.length == 1 ? params[0] : params);
        LOG.info("[{}] START {}.{}", requestId, serviceId, operation);
        long startNanos = System.nanoTime();
        try {
            T result = target.execute(operation, params);
            long durationMs = elapsedMs(startNanos);
            logRepository.save(new LogEntry(requestId, serviceId, operation, durationMs, LogStatus.SUCCESS,
                    Instant.now(), paramsSnapshot, sanitizer.sanitize(result), null, null, null));
            LOG.info("[{}] END {}.{} SUCCESS in {} ms", requestId, serviceId, operation, durationMs);
            return result;
        } catch (RuntimeException error) {
            long durationMs = elapsedMs(startNanos);
            logRepository.save(new LogEntry(requestId, serviceId, operation, durationMs, LogStatus.ERROR,
                    Instant.now(), paramsSnapshot, null, error.getClass().getSimpleName(), error.getMessage(),
                    StackTraceSummarizer.summarize(error)));
            LOG.warn("[{}] END {}.{} ERROR in {} ms: {}", requestId, serviceId, operation, durationMs,
                    error.getMessage());
            throw error;
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
