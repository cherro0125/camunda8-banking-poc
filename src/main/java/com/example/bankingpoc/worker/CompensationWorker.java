package com.example.bankingpoc.worker;

import com.example.bankingpoc.client.NotificationClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import org.springframework.stereotype.Component;

@Component
public class CompensationWorker {

    private final NotificationClient notificationClient;

    public CompensationWorker(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @JobWorker(type = "compensate-disbursement")
    public void compensate(@Variable String customerId) {
        notificationClient.notifyCustomer(customerId,
            "Your loan disbursement could not be completed. Our team has been notified.");
        // void return -> auto-completes with no variables
    }
}
