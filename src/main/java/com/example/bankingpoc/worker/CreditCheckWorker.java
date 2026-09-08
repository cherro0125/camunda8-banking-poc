package com.example.bankingpoc.worker;

import com.example.bankingpoc.client.CreditBureauClient;
import com.example.bankingpoc.client.CreditBureauTimeoutException;
import com.example.bankingpoc.dto.CreditReport;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.exception.CamundaError;

import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CreditCheckWorker {

    private final CreditBureauClient creditBureauClient;

    public CreditCheckWorker(CreditBureauClient creditBureauClient) {
        this.creditBureauClient = creditBureauClient;
    }

    @JobWorker(type = "credit-check")
    public Map<String, Object> checkCredit(@Variable String customerId) {
        try {
            CreditReport report = creditBureauClient.fetchReport(customerId);
            return Map.of(
                "creditScore", report.score(),
                "monthlyIncome", report.monthlyIncome(),
                "existingDebtRatio", report.existingDebtRatio(),
                "regionRiskScore", report.regionRiskScore()
            );
        } catch (CreditBureauTimeoutException e) {
            // Transient failure: bureau API timed out - retry with backoff rather than
            // burning an incident immediately.
            throw CamundaError.jobError(
                "Credit bureau timeout for customer " + customerId,
                Map.of(),
                null,                       // default: currentRetries - 1
                Duration.ofSeconds(30),
                e
            );
        }
    }
}
