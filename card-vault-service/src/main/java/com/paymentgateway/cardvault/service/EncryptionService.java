package com.paymentgateway.cardvault.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM encryption service.
 *
 * WHY AES-256-GCM specifically?
 * - AES-256: 256-bit key, computationally infeasible to brute force
 * - GCM mode: provides both confidentiality AND authenticity
 *   If ciphertext is tampered with, decryption throws an exception
 *   CBC mode only provides confidentiality — no tamper detection
 *
 * HOW AES-GCM works:
 * 1. Generate random 12-byte IV (initialization vector)
 * 2. Use IV + key to initialize the cipher
 * 3. Encrypt plaintext → produces ciphertext + auth tag
 * 4. Store ciphertext and IV (IV is not secret)
 * 5. To decrypt: use same IV + key to reverse the process
 *
 * WHY random IV every time?
 * If you encrypt the same PAN twice with the same IV,
 * an attacker can detect that two tokens belong to the
 * same card even without knowing the key. Random IV
 * makes every encryption unique.
 */
@Slf4j
@Service
public class EncryptionService {

    // GCM standard: 12 bytes IV, 128-bit auth tag
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    /**
     * Encrypts plaintext using AES-256-GCM.
     *
     * @param plaintext  the sensitive data to encrypt (PAN, expiry)
     * @param keyBytes   the 32-byte AES-256 key from HashiCorp Vault
     * @return EncryptionResult containing base64-encoded ciphertext and IV
     */
    public EncryptionResult encrypt(String plaintext, byte[] keyBytes) {
        try {
            // Generate cryptographically random IV
            // SecureRandom uses OS entropy — not Math.random()
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Build the cipher
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            // Base64 encode for storage — binary data in DB columns
            // needs to be text-safe
            return new EncryptionResult(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(iv)
            );

        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        } finally {
            // Zero out key bytes from memory immediately
            // WHY: Java GC doesn't guarantee when objects are cleared.
            // Explicitly zeroing prevents the key from sitting in
            // memory longer than necessary.
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    /**
     * Decrypts AES-256-GCM ciphertext.
     * Throws exception if ciphertext was tampered with.
     *
     * @param ciphertext  base64-encoded encrypted data
     * @param ivBase64    base64-encoded IV used during encryption
     * @param keyBytes    the 32-byte AES-256 key from HashiCorp Vault
     * @return decrypted plaintext
     */
    public String decrypt(String ciphertext, String ivBase64, byte[] keyBytes) {
        try {
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] encryptedData = Base64.getDecoder().decode(ciphertext);

            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            // If ciphertext was tampered with, this throws
            // AEADBadTagException — tamper detection in action
            byte[] decrypted = cipher.doFinal(encryptedData);
            return new String(decrypted);

        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        } finally {
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    /**
     * Detects card brand from first digits (IIN/BIN range).
     * Safe to do on raw PAN before clearing memory.
     */
    public String detectCardBrand(String pan) {
        if (pan.startsWith("4")) return "VISA";
        if (pan.startsWith("5") || pan.startsWith("2")) return "MASTERCARD";
        if (pan.startsWith("34") || pan.startsWith("37")) return "AMEX";
        if (pan.startsWith("6")) return "RUPAY";
        return "UNKNOWN";
    }

    /**
     * Immutable result of an encryption operation.
     */
    public record EncryptionResult(String ciphertext, String iv) {}
}