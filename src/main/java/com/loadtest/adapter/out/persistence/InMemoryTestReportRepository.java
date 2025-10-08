package com.loadtest.adapter.out.persistence;

import com.loadtest.application.port.out.TestReportRepository;
import com.loadtest.domain.model.TestReport;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTestReportRepository implements TestReportRepository {

    private final ConcurrentHashMap<UUID, TestReport> store = new ConcurrentHashMap<>();

    @Override
    public void save(UUID runId, TestReport report) {
        store.put(runId, report);
    }

    @Override
    public Optional<TestReport> findByRunId(UUID runId) {
        return Optional.ofNullable(store.get(runId));
    }
}
