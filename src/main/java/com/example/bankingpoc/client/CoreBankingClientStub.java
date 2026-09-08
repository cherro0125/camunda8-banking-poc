package com.example.bankingpoc.client;

import com.example.bankingpoc.dto.DisbursementResult;
import org.springframework.stereotype.Component;

/**
 * PoC stub - always succeeds unless the customer id contains "fail", which
 * simulates a core-banking rejection (exercises the boundary error / compensation
 * path in {@link com.example.bankingpoc.worker.DisbursementWorker}).
 */
@Component
public class CoreBankingClientStub implements CoreBankingClient {

    @Override
    public DisbursementResult transfer(String customerId, double amount, String idempotencyKey) {
        if (customerId != null && customerId.toLowerCase().contains("fail")) {
            return DisbursementResult.failure("Core banking rejected transfer for " + customerId);
        }
        return DisbursementResult.success("tx-" + idempotencyKey);
    }
}
