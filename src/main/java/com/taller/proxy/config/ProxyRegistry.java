package com.taller.proxy.config;

import com.taller.proxy.exception.UnknownServiceException;
import com.taller.proxy.proxy.MicroserviceProxy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maps a serviceId (inventory | orders | payments) to the logging proxy that fronts that service. */
public class ProxyRegistry {

    private final Map<String, MicroserviceProxy<Object>> proxies;

    public ProxyRegistry(Map<String, MicroserviceProxy<Object>> proxies) {
        this.proxies = new LinkedHashMap<>(proxies);
    }

    public MicroserviceProxy<Object> require(String serviceId) {
        MicroserviceProxy<Object> proxy = proxies.get(serviceId);
        if (proxy == null) {
            throw new UnknownServiceException(serviceId, proxies.keySet());
        }
        return proxy;
    }

    public boolean contains(String serviceId) {
        return proxies.containsKey(serviceId);
    }

    public List<String> serviceIds() {
        return List.copyOf(proxies.keySet());
    }
}
