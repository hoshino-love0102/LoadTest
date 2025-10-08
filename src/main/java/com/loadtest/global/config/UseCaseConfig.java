package com.loadtest.global.config;

import com.loadtest.application.port.in.RunUseCase;
import com.loadtest.application.port.in.TestUseCase;
import com.loadtest.application.port.out.*;
import com.loadtest.application.service.RunService;
import com.loadtest.application.service.TestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public TestUseCase testUseCase(TestDefinitionRepository testDefinitionRepository) {
        return new TestService(testDefinitionRepository);
    }

    @Bean
    public RunUseCase runUseCase(TestDefinitionRepository testDefinitionRepository,
                                 TestRunRepository testRunRepository,
                                 RunRuntimeStore runtimeStore,
                                 LoadTestRunner loadTestRunner,
                                 TestReportRepository reportRepository) {
        return new RunService(testDefinitionRepository, testRunRepository, runtimeStore, loadTestRunner, reportRepository);
    }

}
