package com.taller.proxy.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class JacksonPayloadSanitizer implements PayloadSanitizer {

    private final ObjectMapper mapper;

    public JacksonPayloadSanitizer(ObjectMapper mapper) {
        // Exact BigDecimals keep amounts such as 2970000 from being rendered as 2.97E+6 in the logs.
        this.mapper = mapper.copy().setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
    }

    @Override
    public JsonNode sanitize(Object value) {
        if (value == null) {
            return NullNode.getInstance();
        }
        try {
            return mapper.valueToTree(value);
        } catch (IllegalArgumentException e) {
            return TextNode.valueOf(String.valueOf(value));
        }
    }
}
