package com.paymentgateway.ledger.repository;

import com.paymentgateway.ledger.domain.LedgerTransaction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface LedgerTransactionRepository
        extends ReactiveCrudRepository<LedgerTransaction, UUID> {

    Mono<LedgerTransaction> findByPaymentId(UUID paymentId);

    Flux<LedgerTransaction> findByMerchantId(String merchantId);
}