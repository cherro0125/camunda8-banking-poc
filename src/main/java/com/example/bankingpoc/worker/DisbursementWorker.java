package com.example.bankingpoc.worker;

import com.example.bankingpoc.client.CoreBankingClient;
import com.example.bankingpoc.dto.DisbursementResult;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.exception.CamundaError;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DisbursementWorker {

    private final CoreBankingClient coreBankingClient;

    public DisbursementWorker(CoreBankingClient coreBankingClient) {
        this.coreBankingClient = coreBankingClient;
    }

    @JobWorker(type = "disburse-funds")
    public Map<String, Object> disburse(@Variable String customerId, @Variable Double amount, ActivatedJob job) {
        // job.getKey() as the idempotency token - at-least-once delivery means this can run twice
        String idempotencyKey = "loan-" + job.getKey();
        DisbursementResult result = coreBankingClient.transfer(customerId, amount, idempotencyKey);

        if (!result.success()) {
            // Modelled business outcome, not a bug - caught by the boundary error event on "Disburse funds"
            throw CamundaError.bpmnError("DISBURSEMENT_FAILED", result.failureReason());
        }
        return Map.of("disbursementRef", result.transactionRef());
    }
}
