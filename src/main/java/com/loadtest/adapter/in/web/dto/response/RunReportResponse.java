package com.loadtest.adapter.in.web.dto.response;

public record RunReportResponse(
        String state,
        TestReportResponse report
) {}
