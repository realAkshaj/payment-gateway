package com.paymentgateway.cardvault.controller;

import com.paymentgateway.cardvault.dto.TokenizeRequest;
import com.paymentgateway.cardvault.dto.TokenizeResponse;
import com.paymentgateway.cardvault.service.CardVaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for card tokenization.
 *
 * WHY is this service on port 8082 and not exposed
 * through the API Gateway directly?
 * The Card Vault is an INTERNAL service. Only the
 * Payment Service calls it — never a public client.
 * In production it would be on a private network with
 * no public ingress at all.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/vault")
@RequiredArgsConstructor
public class CardVaultController {

    private final CardVaultService cardVaultService;

    /**
     * Tokenizes a raw card number.
     * Returns a token safe to store and share.
     */
    @PostMapping("/tokenize")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TokenizeResponse> tokenize(
            @Valid @RequestBody TokenizeRequest request
    ) {
        return cardVaultService.tokenize(request);
    }

    /**
     * Validates a token — used by Payment Service
     * before authorizing a charge.
     */
    @GetMapping("/validate/{token}")
    public Mono<Boolean> validateToken(
            @PathVariable String token,
            @RequestParam String merchantId
    ) {
        return cardVaultService.validateToken(token, merchantId);
    }
}