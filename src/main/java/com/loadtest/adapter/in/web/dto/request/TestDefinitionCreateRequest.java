package com.loadtest.adapter.in.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record TestDefinitionCreateRequest(
        @NotBlank String name,
        @NotBlank String method,
        @NotBlank String url,
        Map<String, Object> headers,
        Object body,
        @Min(1) int vus,
        @Min(1) int durationSec,
        Integer rampUpSec
) {}
