package com.loadtest.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.UUID;

public record RunStatusResponse(
        UUID runId,
        UUID testId,
        String status,
        Instant startedAt,
        Instant endedAt,
        TestReportResponse snapshot
) {}
