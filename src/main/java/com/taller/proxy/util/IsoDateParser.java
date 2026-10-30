package com.taller.proxy.util;

import com.taller.proxy.exception.InvalidParameterException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/** Parses ISO-8601 instants, offset date-times, local date-times (UTC) or plain dates (UTC). */
public final class IsoDateParser {

    private IsoDateParser() {
    }

    /**
     * @param endOfRange when the value is a plain date, resolve to the last instant of that day
     *                   (used for the {@code to} bound) instead of its first instant
     */
    public static Instant parse(String name, String value, boolean endOfRange) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        try {
            if (text.length() == 10) {
                LocalDate date = LocalDate.parse(text);
                return endOfRange
                        ? date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1)
                        : date.atStartOfDay().toInstant(ZoneOffset.UTC);
            }
            try {
                return Instant.parse(text);
            } catch (DateTimeParseException ignored) {
                // fall through to the other accepted formats
            }
            try {
                return OffsetDateTime.parse(text).toInstant();
            } catch (DateTimeParseException ignored) {
                return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
            }
        } catch (DateTimeParseException e) {
            throw new InvalidParameterException("Invalid ISO-8601 value for '" + name + "': " + value);
        }
    }
}
