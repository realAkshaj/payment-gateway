package com.paymentgateway.cardvault.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.Base64;
import java.util.Map;

/**
 * Fetches the AES encryption key from HashiCorp Vault.
 *
 * WHY fetch from Vault instead of config/env vars?
 * 1. The key is never in your codebase or config files
 * 2. The key is never in environment variables (visible in ps)
 * 3. Every key access is logged by Vault for audit
 * 4. Keys can be rotated without redeploying the app
 * 5. Vault can revoke access instantly if compromised
 *
 * HOW it works:
 * Vault stores key-value secrets at paths like secret/card-vault
 * We store the AES key as a base64-encoded string at that path
 * At startup we fetch it and keep it in memory (not in a field)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VaultKeyService {

    private final VaultTemplate vaultTemplate;

    private static final String VAULT_PATH = "secret/data/card-vault";
    private static final String KEY_NAME = "aes-key";

    /**
     * Fetches the AES-256 key from HashiCorp Vault.
     *
     * Returns a fresh byte array each time — callers must
     * zero it out after use (Arrays.fill(keyBytes, (byte) 0))
     *
     * WHY return byte[] and not String?
     * Strings in Java are immutable and interned — you can't
     * zero them out from memory. byte[] can be explicitly
     * cleared after use, minimizing the window the key
     * exists in memory.
     */
    public byte[] getEncryptionKey() {
        log.debug("Fetching encryption key from Vault");

        VaultResponse response = vaultTemplate.read(VAULT_PATH);

        if (response == null || response.getData() == null) {
            throw new RuntimeException(
                    "Failed to fetch encryption key from Vault.");
        }

        // KV v2 wraps the actual data in a nested "data" map
        // Structure: response.data.data.aes-key
        @SuppressWarnings("unchecked")
        Map<String, Object> innerData = (Map<String, Object>)
                response.getData().get("data");

        if (innerData == null) {
            throw new RuntimeException("No data found at vault path: " + VAULT_PATH);
        }

        String base64Key = (String) innerData.get(KEY_NAME);

        if (base64Key == null) {
            throw new RuntimeException(
                    "Key '" + KEY_NAME + "' not found at path: " + VAULT_PATH);
        }

        return Base64.getDecoder().decode(base64Key);
    }
}