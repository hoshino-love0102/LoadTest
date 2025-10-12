package com.loadtest.adapter.in.web.dto.response;

public record TargetRejectedResponse(
        String error,
        String reason,
        String message
) {}
