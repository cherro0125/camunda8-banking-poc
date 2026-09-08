package com.example.bankingpoc.client;

import com.example.bankingpoc.dto.CreditReport;
import org.springframework.stereotype.Component;

/**
 * PoC stub - deterministic fake bureau data derived from the customer id, so the
 * same id always produces the same score. Replace with a real client for production use.
 *
 * <p>Demo hooks: a customer id containing "timeout" simulates a bureau timeout
 * (exercises the transient-failure / retry path in {@link com.example.bankingpoc.worker.CreditCheckWorker}).
 */
@Component
public class CreditBureauClientStub implements CreditBureauClient {

    @Override
    public CreditReport fetchReport(String customerId) {
        if (customerId != null && customerId.toLowerCase().contains("timeout")) {
            throw new CreditBureauTimeoutException("Credit bureau did not respond in time for " + customerId);
        }

        int hash = Math.abs(customerId == null ? 0 : customerId.hashCode());
        int creditScore = 300 + (hash % 551);                    // 300-850
        double monthlyIncome = 1000 + (hash % 91) * 100.0;        // 1000-10000
        double existingDebtRatio = (hash % 81) / 100.0;           // 0.00-0.80
        int regionRiskScore = hash % 101;                         // 0-100

        return new CreditReport(creditScore, monthlyIncome, existingDebtRatio, regionRiskScore);
    }
}
