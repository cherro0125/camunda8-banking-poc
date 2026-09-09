package com.example.bankingpoc.simulator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stands in for the real core banking system's Kafka consumer/producer pair, since no
 * such system exists for this PoC. In production this class would not exist - a
 * separate, real core banking service would consume "disbursement-requests" and
 * produce "disbursement-confirmations" on its own.
 *
 * <p>Consumes {@code disbursement-requests} (published by the process via the Kafka
 * Outbound Connector on Task_PublishDisbursementRequest) and produces
 * {@code disbursement-confirmations} (consumed by the process via the Kafka Inbound
 * Connector on Event_WaitDisbursementConfirmation), simulating an async transfer.
 *
 * <p>Demo hook: a customer id containing "fail" produces a failure confirmation,
 * exercising the Gateway_DisbursementOutcome -> Task_Compensate path.
 *
 * <p>Disabled in CPT tests ({@code core-banking-simulator.enabled=false} in
 * src/test/resources/application.yaml) - those scenarios correlate the confirmation
 * message directly, no real Kafka broker involved.
 */
@Component
@ConditionalOnProperty(prefix = "core-banking-simulator", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CoreBankingKafkaSimulator {

    private static final Logger LOG = LoggerFactory.getLogger(CoreBankingKafkaSimulator.class);
    private static final String CONFIRMATIONS_TOPIC = "disbursement-confirmations";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public CoreBankingKafkaSimulator(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "disbursement-requests", groupId = "core-banking-simulator")
    public void onDisbursementRequested(String requestJson) throws Exception {
        JsonNode request = objectMapper.readTree(requestJson);
        String customerId = request.path("customerId").asText();
        double amount = request.path("amount").asDouble();

        LOG.info("Core banking simulator received disbursement request for {} ({})", customerId, amount);

        ObjectNode confirmation = objectMapper.createObjectNode();
        confirmation.put("customerId", customerId);

        if (customerId.toLowerCase().contains("fail")) {
            confirmation.put("disbursementSuccess", false);
            confirmation.put("failureReason", "Core banking rejected transfer for " + customerId);
        } else {
            confirmation.put("disbursementSuccess", true);
            confirmation.put("disbursementRef", "tx-" + UUID.randomUUID());
        }

        kafkaTemplate.send(CONFIRMATIONS_TOPIC, customerId, objectMapper.writeValueAsString(confirmation));
        LOG.info("Core banking simulator published disbursement confirmation for {}: success={}",
            customerId, confirmation.get("disbursementSuccess").asBoolean());
    }
}
