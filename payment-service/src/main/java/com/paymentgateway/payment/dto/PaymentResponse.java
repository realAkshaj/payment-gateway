package com.paymentgateway.payment.dto;

import com.paymentgateway.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Outbound response after creating a payment.
 *
 * Notice what's NOT here:
 * - No cardToken (security)
 * - No correlationId (internal)
 * - No failureReason on successful payments
 *
 * We return 202 ACCEPTED, not 201 CREATED.
 * WHY 202 and not 201?
 * 201 means "the thing is fully created and ready."
 * 202 means "we accepted your request and are processing it."
 * A payment isn't complete when we return — the SAGA is still
 * running across multiple services. 202 is the honest response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID paymentId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String merchantId;
    private String idempotencyKey;
    private Instant createdAt;

    // Only populated if status = FAILED
    private String failureReason;
}