package com.taller.proxy.dto;

/** Envelope returned by every service endpoint: the operation result plus the audit requestId. */
public record ServiceResponse(String requestId, String serviceId, String operation, Object result) {
}
