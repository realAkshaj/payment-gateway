package com.paymentgateway.cardvault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response after successful tokenization.
 *
 * Notice what is NOT here:
 * - No PAN
 * - No expiry in plaintext
 * - No encryption details
 *
 * The token is all the caller ever needs.
 * Everything else stays inside the PCI boundary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenizeResponse {

    private UUID tokenId;
    private String token;        // tok_{uuid} — safe to store anywhere
    private String last4Digits;  // for display only
    private String cardBrand;    // VISA, MASTERCARD etc.
    private Instant createdAt;
}