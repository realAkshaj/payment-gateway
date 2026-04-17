package com.paymentgateway.common.events;

import com.paymentgateway.common.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Published by Payment Service when a new payment is created.
 * This is the event that kicks off the entire SAGA.
 *
 * WHY BigDecimal for amount?
 * Never use double or float for money. Floating point arithmetic
 * is imprecise — 0.1 + 0.2 = 0.30000000000000004 in Java.
 * BigDecimal is exact. This is non-negotiable in financial systems.
 *
 * WHY String cardToken and not card details?
 * By the time this event is published, the Card Vault has already
 * tokenized the raw PAN. Raw card data NEVER appears in Kafka events.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentInitiatedEvent extends BaseEvent {

    private String merchantId;
    private String customerId;
    private BigDecimal amount;
    private String currency;         // ISO 4217: "INR", "USD"
    private PaymentMethod paymentMethod;
    private String cardToken;        // from Card Vault — never raw PAN
    private String idempotencyKey;   // client-provided, prevents double charge
}