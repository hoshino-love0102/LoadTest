package com.loadtest.adapter.in.web.controller;

import com.loadtest.adapter.in.web.assembler.RunResponseAssembler;
import com.loadtest.adapter.in.web.dto.response.*;
import com.loadtest.application.port.in.RunUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class RunController {

    private final RunUseCase runUseCase;
    private final RunResponseAssembler assembler = new RunResponseAssembler();

    public RunController(RunUseCase runUseCase) {
        this.runUseCase = runUseCase;
    }

    @PostMapping("/tests/{testId}/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public StartRunResponse start(@PathVariable UUID testId) {
        UUID runId = runUseCase.start(testId);
        return assembler.toStartResponse(runId);
    }

    @GetMapping("/runs/{runId}")
    public RunStatusResponse status(@PathVariable UUID runId) {
        var run = runUseCase.getStatus(runId);
        var snap = runUseCase.getReport(runId);
        return assembler.toStatusResponse(run, snap);
    }

    @GetMapping("/runs/{runId}/report")
    public RunReportResponse report(@PathVariable UUID runId) {
        var run = runUseCase.getStatus(runId);
        var rep = runUseCase.getReport(runId);
        return assembler.toReportResponseWrapper(run.status().name(), rep);
    }

    @PostMapping("/runs/{runId}/stop")
    public StopRunResponse stop(@PathVariable UUID runId) {
        runUseCase.stop(runId);
        var run = runUseCase.getStatus(runId);
        return assembler.toStopResponse(run);
    }

    @GetMapping("/runs/{runId}/timeseries")
    public RunTimeSeriesResponse timeseries(@PathVariable UUID runId) {
        var samples = runUseCase.getTimeSeries(runId);
        return assembler.toTimeSeriesResponse(samples);
    }
}
