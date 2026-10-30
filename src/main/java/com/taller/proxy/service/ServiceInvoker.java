package com.taller.proxy.service;

import com.taller.proxy.config.ProxyRegistry;
import com.taller.proxy.dto.ServiceResponse;
import com.taller.proxy.proxy.RequestContext;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Routes an API call to the logging proxy of the requested service. */
@Service
public class ServiceInvoker {

    private final ProxyRegistry registry;

    public ServiceInvoker(ProxyRegistry registry) {
        this.registry = registry;
    }

    public ServiceResponse invoke(String serviceId, String operation, Map<String, Object> params) {
        Object result = registry.require(serviceId).execute(operation, params == null ? Map.of() : params);
        String requestId = RequestContext.current().orElse(null);
        return new ServiceResponse(requestId, serviceId, operation, result);
    }
}
