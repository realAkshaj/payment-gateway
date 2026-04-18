package com.paymentgateway.payment.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handling for all controllers.
 *
 * WHY @RestControllerAdvice?
 * Without this, every controller method would need its own
 * try/catch. With this, exceptions bubble up naturally and
 * this class intercepts them before Spring sends the response.
 *
 * WHY return Map<String, Object> instead of a custom class?
 * For simplicity at this stage. In production you'd have a
 * proper ErrorResponse DTO in the common module.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles @Valid validation failures.
     * Returns 400 with a map of field → error message.
     *
     * Example response:
     * {
     *   "status": 400,
     *   "errors": {
     *     "merchantId": "Merchant ID is required",
     *     "amount": "Amount must be greater than 0"
     *   }
     * }
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidationErrors(
            WebExchangeBindException ex
    ) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("status", 400);
        body.put("error", "Validation failed");
        body.put("errors", fieldErrors);
        body.put("timestamp", Instant.now().toString());

        log.warn("Validation failed: {}", fieldErrors);

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body));
    }

    /**
     * Catch-all for unexpected exceptions.
     * Returns 500 without leaking internal details.
     *
     * WHY not return ex.getMessage()?
     * Stack traces and internal error messages can reveal
     * system internals to attackers. Log the details,
     * return a generic message to the client.
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericError(
            Exception ex
    ) {
        log.error("Unexpected error", ex);

        Map<String, Object> body = new HashMap<>();
        body.put("status", 500);
        body.put("error", "Internal server error");
        body.put("timestamp", Instant.now().toString());

        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body));
    }
}