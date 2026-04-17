package com.paymentgateway.common.exceptions;

/**
 * Base exception for all payment-related errors.
 * Services throw specific subclasses of this.
 *
 * WHY a custom exception hierarchy?
 * So the API Gateway can catch PaymentException and return
 * a proper 4xx/5xx response without leaking internal stack traces.
 */
public class PaymentException extends RuntimeException {

    private final String errorCode;

    public PaymentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PaymentException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}