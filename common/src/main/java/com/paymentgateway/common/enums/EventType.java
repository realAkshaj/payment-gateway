package com.paymentgateway.common.enums;

/**
 * Every Kafka event type in the system.
 *
 * WHY: Every message on Kafka carries one of these types.
 * Services use this to route events to the right handler
 * without deserializing the full payload first.
 *
 * Naming convention: ENTITY_ACTION
 * PAYMENT_INITIATED   → Payment Service → all subscribers
 * PAYMENT_AUTHORIZED  → Card Vault → Payment Service
 * PAYMENT_CAPTURED    → Payment Service → Ledger Service
 * PAYMENT_FAILED      → any service → Payment Service (triggers SAGA compensation)
 * PAYMENT_REVERSED    → Payment Service → Ledger Service (compensating transaction)
 * LEDGER_UPDATED      → Ledger Service → Webhook Service
 * WEBHOOK_DELIVERED   → Webhook Service (internal tracking)
 * WEBHOOK_FAILED      → Webhook Service → DLQ
 */
public enum EventType {
    PAYMENT_INITIATED,
    PAYMENT_AUTHORIZED,
    PAYMENT_CAPTURED,
    PAYMENT_FAILED,
    PAYMENT_REVERSED,
    LEDGER_UPDATED,
    WEBHOOK_DELIVERED,
    WEBHOOK_FAILED
}