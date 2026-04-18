package com.paymentgateway.ledger.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.common.events.PaymentInitiatedEvent;
import com.paymentgateway.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for payment events.
 *
 * WHY @KafkaListener and not reactive Kafka?
 * Spring Kafka's @KafkaListener is battle-tested and simple.
 * Reactive Kafka exists but adds complexity without much
 * benefit for this use case — we're not streaming millions
 * of events per second in the ledger.
 *
 * The consumer group ID is "ledger-service".
 * WHY matters: Kafka delivers each message to ONE consumer
 * per group. If you run 3 ledger instances, Kafka distributes
 * partitions across them — each payment processed exactly once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;

    /**
     * Listens for payment initiated events.
     *
     * WHY record on PAYMENT_INITIATED and not PAYMENT_CAPTURED?
     * For this learning project we record immediately.
     * In production you'd wait for PAYMENT_CAPTURED to ensure
     * the card was actually authorized before posting to ledger.
     */
    @KafkaListener(
            topics = "payment.initiated",
            groupId = "ledger-service"
    )
    public void handlePaymentInitiated(String message) {
        try {
            log.info("Received payment event from Kafka");

            // Debezium wraps payload in {"schema":..., "payload":"..."}
            // We need to extract the inner payload string first
            com.fasterxml.jackson.databind.JsonNode root =
                    objectMapper.readTree(message);

            String payloadJson;
            if (root.has("payload")) {
                // Debezium envelope — extract inner payload
                payloadJson = root.get("payload").asText();
            } else {
                // Direct JSON — use as-is
                payloadJson = message;
            }

            PaymentInitiatedEvent event = objectMapper
                    .readValue(payloadJson, PaymentInitiatedEvent.class);

            log.info("Processing payment — paymentId={} amount={}",
                    event.getPaymentId(), event.getAmount());

            ledgerService.recordPayment(
                    event.getPaymentId(),
                    event.getMerchantId(),
                    event.getCustomerId(),
                    event.getAmount(),
                    event.getCurrency()
            ).subscribe(
                    tx -> log.info("Ledger entry created — transactionId={}",
                            tx.getId()),
                    error -> log.error("Failed to record ledger entry",
                            error)
            );

        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage());
        }
    }
}