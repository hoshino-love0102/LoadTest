package com.loadtest.application.service;

import com.loadtest.application.port.in.RunUseCase;
import com.loadtest.application.port.out.*;
import com.loadtest.domain.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

public class RunService implements RunUseCase {

    private final TestDefinitionRepository testDefinitionRepository;
    private final TestRunRepository testRunRepository;
    private final RunRuntimeStore runtimeStore;
    private final LoadTestRunner loadTestRunner;
    private final TestReportRepository reportRepository;

    // ✅ 추가
    private final RunSampleRepository sampleRepository;

    // ✅ 샘플러(전역 1개) + run별 task 추적
    private final ScheduledExecutorService sampler = Executors.newScheduledThreadPool(1);
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> samplingTasks = new ConcurrentHashMap<>();

    public RunService(TestDefinitionRepository testDefinitionRepository,
                      TestRunRepository testRunRepository,
                      RunRuntimeStore runtimeStore,
                      LoadTestRunner loadTestRunner,
                      TestReportRepository reportRepository,
                      RunSampleRepository sampleRepository) {
        this.testDefinitionRepository = testDefinitionRepository;
        this.testRunRepository = testRunRepository;
        this.runtimeStore = runtimeStore;
        this.loadTestRunner = loadTestRunner;
        this.reportRepository = reportRepository;
        this.sampleRepository = sampleRepository;
    }

    @Override
    public UUID start(UUID testId) {
        TestDefinition def = testDefinitionRepository.findById(testId)
                .orElseThrow(() -> new IllegalArgumentException("TestDefinition not found: " + testId));

        UUID runId = UUID.randomUUID();

        TestRun run = new TestRun(runId, testId, TestRun.Status.RUNNING, Instant.now(), null);
        testRunRepository.save(run);

        MetricsAggregator agg = new MetricsAggregator();
        Instant deadline = Instant.now().plusSeconds(Math.max(1, def.durationSec()));

        int threads = Math.max(1, def.vus());
        var executor = Executors.newFixedThreadPool(threads);

        RunRuntime runtime = new RunRuntime(runId, testId, deadline, agg, executor);
        runtimeStore.put(runtime);

        // ✅ 샘플링 시작 (1초마다 snapshot 저장)
        startSampling(runId);

        loadTestRunner.start(def, runtime, () -> finishRunDone(runId));

        return runId;
    }

    private void startSampling(UUID runId) {
        samplingTasks.computeIfAbsent(runId, id ->
                sampler.scheduleAtFixedRate(() -> {
                    // runtime 없으면 샘플링 중단
                    var opt = runtimeStore.get(runId);
                    if (opt.isEmpty()) {
                        stopSampling(runId);
                        return;
                    }

                    var rt = opt.get();
                    TestReport rep = rt.aggregator().snapshot();
                    sampleRepository.append(new RunSample(runId, Instant.now(), rep));
                }, 0, 1, TimeUnit.SECONDS)
        );
    }

    private void stopSampling(UUID runId) {
        ScheduledFuture<?> f = samplingTasks.remove(runId);
        if (f != null) f.cancel(false);
    }

    private void finishRunDone(UUID runId) {
        Optional<TestRun> opt = testRunRepository.findById(runId);
        if (opt.isEmpty()) {
            stopSampling(runId);
            cleanupRuntime(runId);
            return;
        }

        TestRun cur = opt.get();
        if (cur.status() != TestRun.Status.RUNNING) {
            // 이미 STOPPED 등으로 바뀐 케이스
            persistFinalReportIfPresent(runId);
            stopSampling(runId);
            cleanupRuntime(runId);
            return;
        }

        // 최종 리포트 저장
        persistFinalReportIfPresent(runId);

        TestRun done = new TestRun(cur.runId(), cur.testId(), TestRun.Status.DONE, cur.startedAt(), Instant.now());
        testRunRepository.update(done);

        stopSampling(runId);
        cleanupRuntime(runId);
    }

    private void persistFinalReportIfPresent(UUID runId) {
        runtimeStore.get(runId).ifPresent(rt -> {
            TestReport finalReport = rt.aggregator().snapshot();
            reportRepository.save(runId, finalReport);
        });
    }

    private void cleanupRuntime(UUID runId) {
        runtimeStore.get(runId).ifPresent(rt -> {
            rt.stopNow();
            rt.executor().shutdownNow();
        });
        runtimeStore.remove(runId);
        // samples는 남겨둠 (UI에서 완료된 run 그래프 보여주기 위함)
    }

    @Override
    public TestRun getStatus(UUID runId) {
        return testRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("TestRun not found: " + runId));
    }

    @Override
    public void stop(UUID runId) {
        TestRun run = testRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("TestRun not found: " + runId));

        if (run.status() != TestRun.Status.RUNNING) return; // idempotent

        // stop전 최종 리포트 저장 + 중단
        runtimeStore.get(runId).ifPresent(rt -> {
            reportRepository.save(runId, rt.aggregator().snapshot());
            rt.stopNow();
            rt.executor().shutdownNow();
        });

        TestRun stopped = new TestRun(run.runId(), run.testId(), TestRun.Status.STOPPED, run.startedAt(), Instant.now());
        testRunRepository.update(stopped);

        stopSampling(runId);
        runtimeStore.remove(runId);
    }

    @Override
    public TestReport getReport(UUID runId) {
        // 실행 중이면 실시간
        return runtimeStore.get(runId)
                .map(rt -> rt.aggregator().snapshot())
                // 끝났으면 최종 리포트
                .orElseGet(() -> reportRepository.findByRunId(runId).orElse(TestReport.empty()));
    }

    @Override
    public List<RunSample> getTimeSeries(UUID runId) {
        return sampleRepository.findByRunId(runId);
    }
}
