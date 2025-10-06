package com.loadtest.domain.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class MetricsAggregator {

    private final LongAdder total = new LongAdder();
    private final LongAdder success = new LongAdder();
    private final LongAdder fail = new LongAdder();

    private final LongAdder sumLatency = new LongAdder();
    private volatile long minLatency = Long.MAX_VALUE;
    private volatile long maxLatency = 0;

    private final ConcurrentHashMap<Integer, LongAdder> statusCounts = new ConcurrentHashMap<>();
    private final LatencyHistogram histogram = new LatencyHistogram();

    public void record(RequestMetricEvent e) {
        total.increment();

        sumLatency.add(e.latencyMs());
        updateMinMax(e.latencyMs());
        histogram.record(e.latencyMs());

        statusCounts.computeIfAbsent(e.statusCode(), k -> new LongAdder()).increment();

        if (e.error()) fail.increment();
        else success.increment();
    }

    public TestReport snapshot() {
        long t = total.sum();
        long s = success.sum();
        long f = fail.sum();
        long sum = sumLatency.sum();

        double avg = (t == 0) ? 0.0 : (double) sum / (double) t;
        long min = (t == 0) ? 0 : minLatency;
        long max = (t == 0) ? 0 : maxLatency;

        Map<Integer, Long> codes = statusCounts.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().sum()
                ));

        return new TestReport(
                t, s, f,
                avg, min, max,
                histogram.percentile(0.50),
                histogram.percentile(0.95),
                histogram.percentile(0.99),
                codes
        );
    }

    private void updateMinMax(long ms) {
        long curMin = minLatency;
        if (ms < curMin) {
            synchronized (this) {
                if (ms < minLatency) minLatency = ms;
            }
        }
        long curMax = maxLatency;
        if (ms > curMax) {
            synchronized (this) {
                if (ms > maxLatency) maxLatency = ms;
            }
        }
    }
}
