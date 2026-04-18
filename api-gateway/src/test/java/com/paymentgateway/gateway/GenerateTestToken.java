package com.paymentgateway.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GenerateTestToken {

    public static void main(String[] args) {
        String secret =
                "mySecretKeyForDevelopmentOnlyNotForProduction123";

        SecretKey key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> claims = new HashMap<>();
        claims.put("merchantId", "merchant_001");

        String token = Jwts.builder()
                .subject("user_001")
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(
                        System.currentTimeMillis() + 86400000L))
                .signWith(key)
                .compact();

        System.out.println("Bearer token:");
        System.out.println(token);
    }
}