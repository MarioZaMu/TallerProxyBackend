package com.taller.proxy.service;

import com.taller.proxy.exception.InvalidParameterException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Typed, validating view over the argument map received by an operation. */
public final class OperationParams {

    private final Map<String, Object> values;

    private OperationParams(Map<String, Object> values) {
        this.values = values;
    }

    /** Uses the first vararg as the argument map when it is a map; otherwise there are no arguments. */
    @SuppressWarnings("unchecked")
    public static OperationParams from(Object[] raw) {
        if (raw != null && raw.length > 0 && raw[0] instanceof Map<?, ?> map) {
            return new OperationParams((Map<String, Object>) map);
        }
        return new OperationParams(Map.of());
    }

    public String requireString(String key) {
        Object value = values.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new InvalidParameterException("'" + key + "' is required and must be a non-empty string");
        }
        return text.trim();
    }

    public String optionalString(String key, String defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof String text) || text.isBlank()) {
            throw new InvalidParameterException("'" + key + "' must be a non-empty string");
        }
        return text.trim();
    }

    public int requirePositiveInt(String key) {
        return positiveInt(key, values.get(key));
    }

    public int optionalPositiveInt(String key, int defaultValue) {
        return values.get(key) == null ? defaultValue : positiveInt(key, values.get(key));
    }

    public BigDecimal requirePositiveAmount(String key) {
        Object value = values.get(key);
        BigDecimal amount;
        try {
            if (value instanceof Number || value instanceof String) {
                amount = new BigDecimal(value.toString());
            } else {
                throw new InvalidParameterException("'" + key + "' is required and must be a number");
            }
        } catch (NumberFormatException e) {
            throw new InvalidParameterException("'" + key + "' must be a valid number");
        }
        if (amount.signum() <= 0) {
            throw new InvalidParameterException("'" + key + "' must be greater than zero");
        }
        return amount;
    }

    public List<OperationParams> requireObjectList(String key) {
        Object value = values.get(key);
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            throw new InvalidParameterException("'" + key + "' is required and must be a non-empty array");
        }
        List<OperationParams> result = new ArrayList<>();
        for (Object element : list) {
            result.add(from(new Object[]{element}));
            if (!(element instanceof Map<?, ?>)) {
                throw new InvalidParameterException("Every element of '" + key + "' must be an object");
            }
        }
        return result;
    }

    private static int positiveInt(String key, Object value) {
        int number;
        try {
            if (value instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
                number = n.intValue();
            } else if (value instanceof String s) {
                number = Integer.parseInt(s.trim());
            } else {
                throw new InvalidParameterException("'" + key + "' is required and must be an integer");
            }
        } catch (NumberFormatException e) {
            throw new InvalidParameterException("'" + key + "' must be a valid integer");
        }
        if (number <= 0) {
            throw new InvalidParameterException("'" + key + "' must be greater than zero");
        }
        return number;
    }
}
