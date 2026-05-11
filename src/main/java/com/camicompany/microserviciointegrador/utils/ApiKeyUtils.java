package com.camicompany.microserviciointegrador.utils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyUtils {

  private static final SecureRandom secureRandom = new SecureRandom();

  public String generatePrefix() {
    return "ak_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
  }

  public String generateSecret() {
    byte[] randomBytes = new byte[32];
    secureRandom.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

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
