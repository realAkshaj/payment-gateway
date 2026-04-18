package com.paymentgateway.payment.repository;

import com.paymentgateway.payment.domain.Payment;
import com.paymentgateway.common.enums.PaymentStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for the payments table.
 *
 * WHY ReactiveCrudRepository and not JpaRepository?
 * JpaRepository is blocking — the thread waits for the DB.
 * ReactiveCrudRepository returns Mono/Flux — the thread is
 * free while waiting. Essential for high-throughput payments.
 *
 * Mono<T>  = 0 or 1 result (like Optional but async)
 * Flux<T>  = 0 to N results (like List but async)
 */
@Repository
public interface PaymentRepository
        extends ReactiveCrudRepository<Payment, UUID> {

    /**
     * The idempotency check query.
     * Before creating any payment, we call this first.
     * If it returns a value, we return that instead of
     * processing the request again.
     *
     * Spring generates:
     * SELECT * FROM payments WHERE idempotency_key = ?
     */
    Mono<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Used by Settlement Service to find all payments
     * ready for the nightly payout batch.
     *
     * Spring generates:
     * SELECT * FROM payments WHERE merchant_id = ? AND status = ?
     */
    Flux<Payment> findByMerchantIdAndStatus(
            String merchantId,
            PaymentStatus status
    );

    /**
     * Used by the SAGA to find all failed payments
     * that need compensation review.
     *
     * Spring generates:
     * SELECT * FROM payments WHERE status = ?
     */
    Flux<Payment> findByStatus(PaymentStatus status);
}