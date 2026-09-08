package com.example.bankingpoc.controller;

import com.example.bankingpoc.dto.LoanRequest;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loans")
public class LoanController {

    private final CamundaClient camundaClient;

    public LoanController(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submitLoanApplication(@RequestBody LoanRequest request) {
        ProcessInstanceEvent instance = camundaClient.newCreateInstanceCommand()
            .bpmnProcessId("LoanApprovalProcess")
            .latestVersion()
            .variables(Map.of(
                "customerId", request.customerId(),
                "amount", request.amount()
            ))
            .send()
            .join();

        return ResponseEntity.ok(Map.of(
            "processInstanceKey", instance.getProcessInstanceKey(),
            "customerId", request.customerId()
        ));
    }
}
