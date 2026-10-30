package com.taller.proxy.controller;

import com.taller.proxy.dto.ServiceResponse;
import com.taller.proxy.service.ServiceInvoker;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** POST /api/services/{inventory|orders|payments}/{operation}: every call goes through a LoggingProxy. */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceInvoker invoker;

    public ServiceController(ServiceInvoker invoker) {
        this.invoker = invoker;
    }

    @PostMapping("/{serviceId}/{operation}")
    public ServiceResponse invoke(@PathVariable String serviceId, @PathVariable String operation,
                                  @RequestBody(required = false) Map<String, Object> body) {
        return invoker.invoke(serviceId, operation, body);
    }
}
