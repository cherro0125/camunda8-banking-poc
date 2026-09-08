package com.example.bankingpoc.client;

/**
 * Integration boundary to whatever notifies the customer (email, SMS, push).
 * Swap the stub implementation for a real client in production.
 */
public interface NotificationClient {

    void notifyCustomer(String customerId, String message);
}
