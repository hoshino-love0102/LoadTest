package com.loadtest.application.port.out;

import com.loadtest.domain.model.TestRun;

import java.util.Optional;
import java.util.UUID;

public interface TestRunRepository {
    void save(TestRun run);
    Optional<TestRun> findById(UUID runId);
    void update(TestRun run);
}
