package com.loadtest.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StopRunResponse(
        UUID runId,
        String status,
        Instant endedAt
) {}
