package com.loadtest.adapter.in.web.dto.response;

import java.util.Map;

public record TestReportResponse(
        long totalRequests,
        long successCount,
        long failCount,
        double avgLatencyMs,
        long minLatencyMs,
        long maxLatencyMs,
        long p50,
        long p95,
        long p99,
        Map<String, Long> statusCodeCounts
) {}
