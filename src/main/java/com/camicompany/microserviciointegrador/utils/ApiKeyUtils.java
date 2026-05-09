package com.camicompany.microserviciointegrador.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Component
public class ApiKeyUtils {

    private static final SecureRandom secureRandom = new SecureRandom();

    // PREFIX: corto y único
    public String generatePrefix() {
        return "ak_" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);
    }

    // SECRET: fuerte y aleatorio
    public String generateSecret() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    // API KEY completa (lo que se muestra al usuario una sola vez)
    public String generateApiKey() {
        return generatePrefix() + "." + generateSecret();
    }

    public String extractPrefix(String apiKey) {

        if (apiKey == null || !apiKey.contains(".")) {
            throw new IllegalArgumentException("Invalid API key format");
        }

        return apiKey.split("\\.")[0];
    }


}
