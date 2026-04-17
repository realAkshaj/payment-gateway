package com.paymentgateway.common.exceptions;

/**
 * Thrown when a duplicate payment request is detected.
 * The caller should receive the ORIGINAL response, not an error.
 * HTTP 200 with the cached result — not HTTP 409.
 *
 * WHY not 409 Conflict?
 * Because from the client's perspective, the payment succeeded.
 * They're just retrying because their network dropped.
 * Returning 200 with the same result is the correct behavior.
 */
public class IdempotencyException extends PaymentException {

    public IdempotencyException(String idempotencyKey) {
        super("DUPLICATE_REQUEST",
                "Duplicate request detected for idempotency key: " + idempotencyKey);
    }
}