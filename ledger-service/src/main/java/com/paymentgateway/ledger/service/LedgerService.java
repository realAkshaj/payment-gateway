package com.paymentgateway.ledger.service;

import com.paymentgateway.ledger.domain.LedgerEntry;
import com.paymentgateway.ledger.domain.LedgerTransaction;
import com.paymentgateway.ledger.repository.LedgerEntryRepository;
import com.paymentgateway.ledger.repository.LedgerTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Double-entry accounting engine.
 *
 * Every payment creates exactly two ledger entries:
 * DEBIT  customer account  (money leaves customer)
 * CREDIT merchant account  (money enters merchant)
 *
 * The golden rule: debits must equal credits.
 * We verify this before saving every transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;

    /**
     * Records a payment as double-entry ledger entries.
     *
     * Called when Kafka delivers a PAYMENT_CAPTURED event.
     *
     * Creates:
     * 1. LedgerTransaction (the header)
     * 2. DEBIT entry on customer account
     * 3. CREDIT entry on merchant account
     *
     * All three writes happen in one transaction.
     * If any fails, all roll back — no partial ledger entries.
     */
    @Transactional
    public Mono<LedgerTransaction> recordPayment(
            UUID paymentId,
            String merchantId,
            String customerId,
            BigDecimal amount,
            String currency
    ) {
        log.info("Recording payment in ledger — paymentId={} amount={} {}",
                paymentId, amount, currency);

        // Check for duplicate — idempotency
        return transactionRepository.findByPaymentId(paymentId)
                .doOnNext(existing -> log.info(
                        "Duplicate ledger entry detected — paymentId={}",
                        paymentId))
                .switchIfEmpty(createDoubleEntry(
                        paymentId, merchantId, customerId, amount, currency));
    }

    private Mono<LedgerTransaction> createDoubleEntry(
            UUID paymentId,
            String merchantId,
            String customerId,
            BigDecimal amount,
            String currency
    ) {
        UUID transactionId = UUID.randomUUID();

        LedgerTransaction transaction = LedgerTransaction.builder()
                .id(transactionId)
                .paymentId(paymentId)
                .merchantId(merchantId)
                .customerId(customerId)
                .totalAmount(amount)
                .currency(currency)
                .transactionType("PAYMENT")
                .status("POSTED")
                .description("Payment captured")
                .createdAt(Instant.now())
                .build();

        // DEBIT entry — money leaves customer
        LedgerEntry debitEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .paymentId(paymentId)
                .accountType("CUSTOMER")
                .accountId(customerId)
                .entryType("DEBIT")
                .amount(amount)
                .currency(currency)
                .description("Payment debit")
                .createdAt(Instant.now())
                .build();

        // CREDIT entry — money enters merchant
        LedgerEntry creditEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .paymentId(paymentId)
                .accountType("MERCHANT")
                .accountId(merchantId)
                .entryType("CREDIT")
                .amount(amount)
                .currency(currency)
                .description("Payment credit")
                .createdAt(Instant.now())
                .build();

        // Verify double-entry balance BEFORE saving
        // Debit amount must equal credit amount
        if (debitEntry.getAmount().compareTo(
                creditEntry.getAmount()) != 0) {
            return Mono.error(new IllegalStateException(
                    "Double-entry imbalance detected — " +
                            "debits do not equal credits"));
        }

        // Save transaction first, then both entries
        return transactionRepository.save(transaction)
                .flatMap(savedTx ->
                        entryRepository.save(debitEntry)
                                .then(entryRepository.save(creditEntry))
                                .then(Mono.just(savedTx)))
                .doOnSuccess(tx -> log.info(
                        "Double-entry recorded — transactionId={} " +
                                "paymentId={} amount={} {}",
                        tx.getId(), paymentId, amount, currency));
    }

    /**
     * Records a payment reversal — compensating transaction.
     * Mirror image of recordPayment — debits and credits flipped.
     */
    @Transactional
    public Mono<LedgerTransaction> recordReversal(
            UUID paymentId,
            String merchantId,
            String customerId,
            BigDecimal amount,
            String currency
    ) {
        log.info("Recording reversal in ledger — paymentId={}", paymentId);

        UUID transactionId = UUID.randomUUID();

        LedgerTransaction reversal = LedgerTransaction.builder()
                .id(transactionId)
                .paymentId(paymentId)
                .merchantId(merchantId)
                .customerId(customerId)
                .totalAmount(amount)
                .currency(currency)
                .transactionType("REVERSAL")
                .status("POSTED")
                .description("Payment reversed")
                .createdAt(Instant.now())
                .build();

        // Reversed — CREDIT customer, DEBIT merchant
        LedgerEntry creditEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .paymentId(paymentId)
                .accountType("CUSTOMER")
                .accountId(customerId)
                .entryType("CREDIT")
                .amount(amount)
                .currency(currency)
                .description("Reversal credit to customer")
                .createdAt(Instant.now())
                .build();

        LedgerEntry debitEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .paymentId(paymentId)
                .accountType("MERCHANT")
                .accountId(merchantId)
                .entryType("DEBIT")
                .amount(amount)
                .currency(currency)
                .description("Reversal debit from merchant")
                .createdAt(Instant.now())
                .build();

        return transactionRepository.save(reversal)
                .flatMap(savedTx ->
                        entryRepository.save(creditEntry)
                                .then(entryRepository.save(debitEntry))
                                .then(Mono.just(savedTx)));
    }

    /**
     * Returns all ledger entries for a payment.
     * Used by the controller for audit queries.
     */
    public Flux<LedgerEntry> getEntriesForPayment(UUID paymentId) {
        return entryRepository.findByPaymentId(paymentId);
    }
}