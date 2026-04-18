package com.paymentgateway.ledger.repository;

import com.paymentgateway.ledger.domain.LedgerEntry;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface LedgerEntryRepository
        extends ReactiveCrudRepository<LedgerEntry, UUID> {

    Flux<LedgerEntry> findByTransactionId(UUID transactionId);

    Flux<LedgerEntry> findByPaymentId(UUID paymentId);

    Flux<LedgerEntry> findByAccountId(String accountId);
}