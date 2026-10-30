package com.taller.proxy.proxy;

import java.util.StringJoiner;

/** Produces a compact stack trace: exception class, message and the top frames. */
public final class StackTraceSummarizer {

    public static final int DEFAULT_FRAMES = 5;

    private StackTraceSummarizer() {
    }

    public static String summarize(Throwable error) {
        return summarize(error, DEFAULT_FRAMES);
    }

    public static String summarize(Throwable error, int maxFrames) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add(error.getClass().getName() + ": " + error.getMessage());
        StackTraceElement[] frames = error.getStackTrace();
        int limit = Math.min(maxFrames, frames.length);
        for (int i = 0; i < limit; i++) {
            joiner.add("    at " + frames[i]);
        }
        if (frames.length > limit) {
            joiner.add("    ... " + (frames.length - limit) + " more");
        }
        return joiner.toString();
    }
}
