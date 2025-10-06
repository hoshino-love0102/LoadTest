package com.loadtest.application.port.out;

import com.loadtest.domain.model.TestDefinition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestDefinitionRepository {
    UUID save(TestDefinition definition);
    Optional<TestDefinition> findById(UUID id);
    List<TestDefinition> findAll();
}
