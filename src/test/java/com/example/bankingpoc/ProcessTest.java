package com.example.bankingpoc;

import io.camunda.process.test.api.CamundaSpringProcessTest;
import io.camunda.process.test.api.testCases.TestCase;
import io.camunda.process.test.api.testCases.TestCaseRunner;
import io.camunda.process.test.api.testCases.TestCaseSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Runs every scenario in src/test/resources/scenarios/*.test.json against an embedded
 * Zeebe engine (via Testcontainers). Resources are deployed automatically through
 * {@link BankingPocApplication}'s own {@code @Deployment} annotation - no
 * {@code @TestDeployment} needed here.
 */
@SpringBootTest
@CamundaSpringProcessTest
public class ProcessTest {

    @Autowired
    private TestCaseRunner testCaseRunner;

    @ParameterizedTest(name = "{0}")
    @TestCaseSource(directory = "/scenarios")
    void shouldPass(final TestCase testCase, final String fileName) {
        testCaseRunner.run(testCase);
    }
}
