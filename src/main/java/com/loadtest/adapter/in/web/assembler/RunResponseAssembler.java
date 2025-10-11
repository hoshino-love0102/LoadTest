package com.loadtest.adapter.in.web.assembler;

import com.loadtest.adapter.in.web.dto.response.*;
import com.loadtest.domain.model.RunSample;
import com.loadtest.domain.model.TestReport;
import com.loadtest.domain.model.TestRun;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class RunResponseAssembler {

    public RunStatusResponse toStatusResponse(TestRun run, TestReport report) {
        return new RunStatusResponse(
                run.runId(),
                run.testId(),
                run.status().name(),
                run.startedAt(),
                run.endedAt(),
                toReportResponse(report)
        );
    }

    public RunReportResponse toReportResponseWrapper(String state, TestReport report) {
        return new RunReportResponse(state, toReportResponse(report));
    }

    public StopRunResponse toStopResponse(TestRun run) {
        return new StopRunResponse(run.runId(), run.status().name(), run.endedAt());
    }

    public StartRunResponse toStartResponse(java.util.UUID runId) {
        return new StartRunResponse(runId);
    }

    public TestReportResponse toReportResponse(TestReport r) {
        Map<String, Long> codes = r.statusCodeCounts().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        Map.Entry::getValue
                ));

        return new TestReportResponse(
                r.totalRequests(),
                r.successCount(),
                r.failCount(),
                r.avgLatencyMs(),
                r.minLatencyMs(),
                r.maxLatencyMs(),
                r.p50(),
                r.p95(),
                r.p99(),
                codes
        );
    }

    public RunTimeSeriesResponse toTimeSeriesResponse(java.util.List<RunSample> samples) {
        var out = new ArrayList<RunSampleResponse>(samples.size());

        RunSample prev = null;
        for (var s : samples) {
            var rep = s.report();

            double rps = calcRps(prev, s);
            double failRate = calcFailRate(rep.totalRequests(), rep.failCount());

            out.add(new RunSampleResponse(
                    s.at(),
                    rep.totalRequests(),
                    rep.successCount(),
                    rep.failCount(),
                    rep.avgLatencyMs(),
                    rep.p50(),
                    rep.p95(),
                    rep.p99(),
                    rps,
                    failRate
            ));
            prev = s;
        }
        return new RunTimeSeriesResponse(out);
    }

    private double calcRps(RunSample prev, RunSample curr) {
        if (prev == null) return 0.0;

        long delta = curr.report().totalRequests() - prev.report().totalRequests();
        long dtMs = Duration.between(prev.at(), curr.at()).toMillis();
        if (dtMs <= 0) dtMs = 1000;

        return (double) delta / ((double) dtMs / 1000.0);
    }

    private double calcFailRate(long total, long fail) {
        if (total <= 0) return 0.0;
        return (double) fail / (double) total;
    }
}
