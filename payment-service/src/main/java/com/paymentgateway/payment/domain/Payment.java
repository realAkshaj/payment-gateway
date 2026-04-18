package com.paymentgateway.payment.domain;

import com.paymentgateway.common.enums.PaymentMethod;
import com.paymentgateway.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Core payment entity — persisted to payments_db.
 *
 * WHY UUID for ID?
 * Sequential IDs (1, 2, 3...) leak business information.
 * If your payment ID is 1042, competitors know you've processed
 * ~1000 payments. UUIDs are opaque and safe to expose publicly.
 *
 * WHY store cardToken and not card details?
 * Raw card data never enters this service. The API Gateway
 * calls Card Vault first, gets a token, then calls us.
 * This keeps Payment Service outside the PCI boundary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payments")
public class Payment {

    @Id
    private UUID id;

    private String merchantId;
    private String customerId;

    // BigDecimal — exact decimal arithmetic, never float/double for money
    private BigDecimal amount;
    private String currency;

    private PaymentStatus status;
    private PaymentMethod paymentMethod;

    // Token from Card Vault — never raw PAN
    private String cardToken;

    // Client-provided — enforces exactly-once processing
    private String idempotencyKey;

    // Correlation ID from API Gateway — traces request across all services
    private String correlationId;

    // Failure reason if status = FAILED
    private String failureReason;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ── State transition methods ──────────────────────────────────────────
    // WHY methods instead of direct field access?
    // So state transitions are explicit and auditable.
    // You can add validation here — e.g. can't go from FAILED to CAPTURED.

    public void markProcessing() {
        this.status = PaymentStatus.PROCESSING;
    }

    public void markAuthorized() {
        this.status = PaymentStatus.AUTHORIZED;
    }

    public void markCaptured() {
        this.status = PaymentStatus.CAPTURED;
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void markReversed() {
        this.status = PaymentStatus.REVERSED;
    }
}