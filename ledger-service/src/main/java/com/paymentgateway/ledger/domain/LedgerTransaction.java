package com.paymentgateway.ledger.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A ledger transaction groups related debit/credit entries.
 *
 * One payment creates one LedgerTransaction with two entries:
 * - DEBIT  on the customer account
 * - CREDIT on the merchant account
 *
 * WHY store totalAmount here if entries have amounts?
 * Denormalization for query performance. Settlement Service
 * needs merchant totals — scanning entries would be slow.
 * Storing the total here makes it an O(1) lookup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("ledger_transactions")
public class LedgerTransaction implements Persistable<UUID> {

    @Id
    private UUID id;

    // The payment that triggered this transaction
    private UUID paymentId;

    private String merchantId;
    private String customerId;
    private BigDecimal totalAmount;
    private String currency;

    // PAYMENT, REVERSAL, REFUND, FEE
    private String transactionType;

    // POSTED = final, PENDING = not yet settled
    private String status;

    private String description;
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNew;
    }
}