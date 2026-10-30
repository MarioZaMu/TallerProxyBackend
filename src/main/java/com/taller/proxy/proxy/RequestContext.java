package com.taller.proxy.proxy;

import java.util.Optional;

/** Holds the requestId of the call being handled by the current thread so that web layers can report it. */
public final class RequestContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(String requestId) {
        CURRENT.set(requestId);
    }

    public static Optional<String> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
