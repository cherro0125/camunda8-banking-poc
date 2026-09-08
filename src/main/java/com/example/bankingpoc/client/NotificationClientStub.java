package com.example.bankingpoc.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** PoC stub - logs instead of sending a real email/SMS/push notification. */
@Component
public class NotificationClientStub implements NotificationClient {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationClientStub.class);

    @Override
    public void notifyCustomer(String customerId, String message) {
        LOG.info("Notifying customer {}: {}", customerId, message);
    }
}
