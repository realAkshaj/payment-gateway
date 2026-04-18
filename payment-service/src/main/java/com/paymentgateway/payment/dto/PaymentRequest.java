package com.paymentgateway.payment.dto;

import com.paymentgateway.common.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Inbound request to create a payment.
 *
 * WHY validation annotations?
 * We want to reject bad requests at the boundary —
 * before they touch the database or any business logic.
 * @NotBlank, @NotNull etc. trigger automatically when
 * Spring sees @Valid on the controller method parameter.
 *
 * The client sends this. We never send it anywhere else.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Merchant ID is required")
    private String merchantId;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3-letter ISO 4217 code")
    private String currency;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // Card token from Card Vault — required for CARD payments
    // null for NET_BANKING
    private String cardToken;

    /**
     * Client-generated unique key for this request.
     * Same key = same request = return cached response.
     *
     * WHY client-generated and not server-generated?
     * Because the client needs to know the key BEFORE sending
     * the request. If the network drops after the server
     * processes but before the client gets the response,
     * the client retries with the SAME key and gets the
     * same result — no double charge.
     */
    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}