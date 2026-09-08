package com.example.bankingpoc.client;

import com.example.bankingpoc.dto.CreditReport;

/**
 * Integration boundary to an external credit bureau. Swap the stub implementation
 * for a real REST/Connector-based client when wiring this PoC to an actual bureau.
 */
public interface CreditBureauClient {

    CreditReport fetchReport(String customerId);
}
