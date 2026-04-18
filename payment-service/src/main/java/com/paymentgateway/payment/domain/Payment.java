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
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payments")
public class Payment implements Persistable<UUID> {

    @Id
    private UUID id;

    private String merchantId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String cardToken;
    private String idempotencyKey;
    private String correlationId;
    private String failureReason;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Tells R2DBC whether to INSERT or UPDATE.
     *
     * WHY: R2DBC checks isNew() before saving.
     * If true  → INSERT (new record)
     * If false → UPDATE (existing record)
     *
     * We return true when createdAt is null because
     * createdAt only gets set AFTER the first save.
     * Before first save = new record = INSERT.
     * After first save = existing record = UPDATE.
     */
    @Override
    @Transient
    public boolean isNew() {
        return createdAt == null;
    }

    // ── State transition methods ──────────────────────────────────────────
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