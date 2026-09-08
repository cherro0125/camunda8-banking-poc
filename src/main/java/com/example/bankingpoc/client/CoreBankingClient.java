package com.example.bankingpoc.client;

import com.example.bankingpoc.dto.DisbursementResult;

/**
 * Integration boundary to the core banking system that actually moves money.
 * Swap the stub implementation for a real REST/Connector-based client in production.
 */
public interface CoreBankingClient {

    DisbursementResult transfer(String customerId, double amount, String idempotencyKey);
}
