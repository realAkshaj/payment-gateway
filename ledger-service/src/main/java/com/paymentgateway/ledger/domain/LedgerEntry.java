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
 * A single debit or credit line in the ledger.
 *
 * Every LedgerTransaction has exactly two entries:
 * one DEBIT and one CREDIT of equal amounts.
 *
 * WHY enforce this at the application level and not DB?
 * We could add a DB trigger, but application-level validation
 * gives better error messages and is easier to test.
 *
 * accountType: CUSTOMER, MERCHANT, PLATFORM, FEE
 * entryType:   DEBIT, CREDIT
 *
 * The double-entry rule:
 * SUM(DEBIT entries) must equal SUM(CREDIT entries)
 * for every transaction. We verify this before saving.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("ledger_entries")
public class LedgerEntry implements Persistable<UUID> {

    @Id
    private UUID id;

    // Parent transaction
    private UUID transactionId;

    // Which payment caused this entry
    private UUID paymentId;

    // CUSTOMER, MERCHANT, PLATFORM
    private String accountType;

    // The specific account ID (merchantId or customerId)
    private String accountId;

    // DEBIT or CREDIT
    private String entryType;

    // Always positive — entryType tells direction
    private BigDecimal amount;
    private String currency;

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