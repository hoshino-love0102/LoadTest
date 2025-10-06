package com.loadtest.domain.model;

public class LatencyHistogram {

    private static final long MAX1 = 5_000;
    private static final long MAX2 = 30_000;
    private static final long MAX3 = 120_000;

    private static final long STEP1 = 10;
    private static final long STEP2 = 100;
    private static final long STEP3 = 1_000;

    private final long[] buckets; // counts
    private long total;

    public LatencyHistogram() {
        int b1 = (int) (MAX1 / STEP1);
        int b2 = (int) ((MAX2 - MAX1) / STEP2);
        int b3 = (int) ((MAX3 - MAX2) / STEP3);
        this.buckets = new long[b1 + b2 + b3 + 1];
        this.total = 0;
    }

    public void record(long latencyMs) {
        buckets[indexOf(latencyMs)]++;
        total++;
    }

    public long totalCount() {
        return total;
    }

    public long percentile(double p) {
        if (total == 0) return 0;
        long rank = (long) Math.ceil(p * total);
        long cum = 0;

        for (int i = 0; i < buckets.length; i++) {
            cum += buckets[i];
            if (cum >= rank) {
                return lowerBoundMs(i);
            }
        }
        return lowerBoundMs(buckets.length - 1);
    }

    private int indexOf(long ms) {
        if (ms < 0) ms = 0;
        if (ms < MAX1) return (int) (ms / STEP1);
        if (ms < MAX2) return offset1() + (int) ((ms - MAX1) / STEP2);
        if (ms < MAX3) return offset2() + (int) ((ms - MAX2) / STEP3);
        return buckets.length - 1; // overflow
    }

    private long lowerBoundMs(int idx) {
        int o1 = offset1();
        int o2 = offset2();
        int o3 = offset3();

        if (idx < o1) return (long) idx * STEP1;
        if (idx < o2) return MAX1 + (long) (idx - o1) * STEP2;
        if (idx < o3) return MAX2 + (long) (idx - o2) * STEP3;
        return MAX3;
    }

    private int offset1() { return (int) (MAX1 / STEP1); }
    private int offset2() { return offset1() + (int) ((MAX2 - MAX1) / STEP2); }
    private int offset3() { return offset2() + (int) ((MAX3 - MAX2) / STEP3); }
}
