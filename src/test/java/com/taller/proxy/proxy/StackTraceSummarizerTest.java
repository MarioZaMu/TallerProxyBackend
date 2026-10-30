package com.taller.proxy.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackTraceSummarizerTest {

    @Test
    void limitsFramesAndReportsTheRest() {
        Throwable error = recurse(10);
        String summary = StackTraceSummarizer.summarize(error, 5);
        assertTrue(summary.startsWith("java.lang.IllegalStateException: boom"));
        assertEquals(5, summary.lines().filter(l -> l.trim().startsWith("at ")).count());
        assertTrue(summary.contains("more"));
    }

    private static Throwable recurse(int depth) {
        if (depth == 0) {
            return new IllegalStateException("boom");
        }
        return recurse(depth - 1);
    }
}
