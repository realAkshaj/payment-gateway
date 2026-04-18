package com.paymentgateway.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ledger Service — double-entry accounting engine.
 *
 * This service maintains an immutable financial record
 * of every transaction in the system.
 *
 * Key rules this service enforces:
 * 1. Every transaction has equal debits and credits
 * 2. No record is ever updated or deleted
 * 3. All entries are append-only
 * 4. Every entry references the payment that caused it
 */
@SpringBootApplication
public class LedgerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
    }
}