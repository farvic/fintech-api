package com.farvic.fintech.http;

public final class IdempotencyHeaders {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String IDEMPOTENCY_REPLAYED = "X-Idempotency-Replayed";
    public static final String IDEMPOTENCY_MESSAGE = "X-Idempotency-Message";

    private IdempotencyHeaders() {
    }
}