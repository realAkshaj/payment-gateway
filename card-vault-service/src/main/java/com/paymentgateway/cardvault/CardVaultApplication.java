package com.paymentgateway.cardvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Card Vault Service — the PCI boundary.
 *
 * This service is the ONLY place in the entire system
 * where raw card numbers (PANs) are ever handled.
 *
 * Everything that enters: raw PAN
 * Everything that leaves: card token
 *
 * Raw PANs never touch:
 * - Kafka messages
 * - Database columns (stored encrypted only)
 * - Log files
 * - Any other service
 */
@SpringBootApplication
public class CardVaultApplication {
    public static void main(String[] args) {
        SpringApplication.run(CardVaultApplication.class, args);
    }
}