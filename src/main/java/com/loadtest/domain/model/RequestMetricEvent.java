package com.loadtest.domain.model;

public record RequestMetricEvent(
        long latencyMs,
        int statusCode,
        boolean error
) {
    public static RequestMetricEvent of(long latencyMs, int statusCode) {
        return new RequestMetricEvent(latencyMs, statusCode, statusCode >= 400);
    }
}
