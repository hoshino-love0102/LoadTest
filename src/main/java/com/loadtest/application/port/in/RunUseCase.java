package com.loadtest.application.port.in;

import com.loadtest.domain.model.TestRun;
import com.loadtest.domain.model.TestReport;
import com.loadtest.domain.model.RunSample;

import java.util.List;
import java.util.UUID;

public interface RunUseCase {
    UUID start(UUID testId);
    TestRun getStatus(UUID runId);
    void stop(UUID runId);
    TestReport getReport(UUID runId);
    List<RunSample> getTimeSeries(UUID runId);
}
