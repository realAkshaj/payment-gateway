package com.paymentgateway.cardvault.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("card_tokens")
public class CardToken implements Persistable<UUID> {

    @Id
    private UUID id;

    private String token;
    private String encryptedPan;
    private String iv;
    private String last4Digits;
    private String cardBrand;
    private String encryptedExpiry;
    private String merchantId;
    private String customerId;
    private Instant createdAt;
    private Instant expiresAt;

    // Explicit new flag — avoids isNew() ambiguity with R2DBC
    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNew;
    }
}