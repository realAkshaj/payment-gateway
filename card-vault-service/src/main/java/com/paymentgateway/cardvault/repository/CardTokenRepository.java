package com.paymentgateway.cardvault.repository;

import com.paymentgateway.cardvault.domain.CardToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CardTokenRepository
        extends ReactiveCrudRepository<CardToken, UUID> {

    Mono<CardToken> findByToken(String token);

    Mono<CardToken> findByTokenAndMerchantId(
            String token,
            String merchantId
    );
}