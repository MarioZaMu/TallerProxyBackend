package com.taller.proxy.proxy;

/**
 * Common contract shared by the real microservices and their proxies (Proxy pattern "Subject").
 * Clients cannot tell whether they talk to the real service or to a proxy.
 *
 * @param <T> type of the value returned by the operations
 */
public interface MicroserviceProxy<T> {

    /**
     * Executes a named operation.
     *
     * @param operation operation name, e.g. {@code listProducts}
     * @param params    operation arguments; by convention the first one is a {@code Map<String, Object>}
     * @return the result of the operation
     */
    T execute(String operation, Object... params);
}
