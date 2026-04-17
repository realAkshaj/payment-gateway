package com.paymentgateway.common.enums;

/**
 * Supported payment methods.
 * CARD → tokenized via Card Vault Service
 * NET_BANKING → direct bank transfer, no card vault needed
 */
public enum PaymentMethod {
    CARD,
    NET_BANKING
}