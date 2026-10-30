package com.taller.proxy.proxy;

import com.fasterxml.jackson.databind.JsonNode;

/** Converts arbitrary objects into an immutable JSON snapshot that is safe to store in a log entry. */
public interface PayloadSanitizer {

    JsonNode sanitize(Object value);
}
