package com.paymentgateway.ledger.controller;

import com.paymentgateway.ledger.domain.LedgerEntry;
import com.paymentgateway.ledger.domain.LedgerTransaction;
import com.paymentgateway.ledger.repository.LedgerTransactionRepository;
import com.paymentgateway.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Read-only API for ledger queries.
 * No write endpoints — all writes come from Kafka events.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;
    private final LedgerTransactionRepository transactionRepository;

    @GetMapping("/payments/{paymentId}")
    public Flux<LedgerEntry> getPaymentEntries(
            @PathVariable UUID paymentId
    ) {
        return ledgerService.getEntriesForPayment(paymentId);
    }

    @GetMapping("/merchants/{merchantId}")
    public Flux<LedgerTransaction> getMerchantTransactions(
            @PathVariable String merchantId
    ) {
        return transactionRepository.findByMerchantId(merchantId);
    }
}