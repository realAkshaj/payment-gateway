package com.paymentgateway.cardvault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound request to tokenize a raw card.
 *
 * This is the ONLY place in the entire codebase where
 * a raw PAN field exists. It enters over TLS, gets
 * tokenized immediately, and is cleared from memory.
 *
 * WHY @Pattern for pan?
 * Basic Luhn format validation before we even touch
 * the encryption. Rejects obviously invalid card numbers
 * at the boundary without storing anything.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenizeRequest {

    @NotBlank(message = "PAN is required")
    @Pattern(regexp = "\\d{13,19}",
            message = "PAN must be 13-19 digits")
    private String pan;             // RAW CARD NUMBER — handle with care

    @NotBlank(message = "Expiry is required")
    @Pattern(regexp = "\\d{2}/\\d{2}",
            message = "Expiry must be MM/YY format")
    private String expiry;          // MM/YY

    @NotBlank(message = "Merchant ID is required")
    private String merchantId;

    @NotBlank(message = "Customer ID is required")
    private String customerId;
}