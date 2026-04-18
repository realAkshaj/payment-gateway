package com.paymentgateway.cardvault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

/**
 * Configuration for Card Vault Service.
 *
 * WHY hardcode the Vault token here?
 * We're using HashiCorp Vault in DEV MODE — it starts
 * with a known root token for local development.
 * In production you'd use Kubernetes service account
 * authentication or AppRole — never a hardcoded token.
 */
@Configuration
@EnableR2dbcAuditing
public class CardVaultConfig {

    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint endpoint = VaultEndpoint.create("localhost", 8200);
        endpoint.setScheme("http"); // dev mode — no TLS locally

        // Dev mode root token — NEVER use in production
        return new VaultTemplate(
                endpoint,
                new TokenAuthentication("dev-root-token")
        );
    }
}