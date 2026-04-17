package com.paymentgateway.common.enums;

/**
 * Represents every state a payment can be in.
 *
 * WHY THIS MATTERS: This is your state machine.
 * A payment must move through these states in order —
 * you can never go from PENDING directly to SETTLED.
 * The Payment Service enforces these transitions.
 *
 * PENDING      → payment record created, not yet processed
 * PROCESSING   → SAGA started, card vault called
 * AUTHORIZED   → card authorized, funds reserved
 * CAPTURED     → funds actually moved
 * SETTLED      → included in nightly settlement batch
 * FAILED       → any step failed, SAGA compensated
 * REVERSED     → authorized but then cancelled
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    AUTHORIZED,
    CAPTURED,
    SETTLED,
    FAILED,
    REVERSED
}