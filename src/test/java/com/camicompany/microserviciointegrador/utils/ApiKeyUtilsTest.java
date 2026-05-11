package com.camicompany.microserviciointegrador.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApiKeyUtilsTest {

  private ApiKeyUtils apiKeyUtils;

  @BeforeEach
  void setUp() {
    apiKeyUtils = new ApiKeyUtils();
  }

  @Test
  void generatePrefixShouldReturnValidPrefix() {
    String prefix = apiKeyUtils.generatePrefix();
    assertNotNull(prefix);
    assertTrue(prefix.startsWith("ak_"));
    assertEquals(11, prefix.length()); // ak_ + 8 chars
  }

  @Test
  void generatePrefixShouldGenerateDifferentPrefixes() {

    String prefix1 = apiKeyUtils.generatePrefix();
    String prefix2 = apiKeyUtils.generatePrefix();
    assertNotEquals(prefix1, prefix2);
  }

  @Test
  void generateSecretShouldReturnBase64String() {
    String secret = apiKeyUtils.generateSecret();
    assertNotNull(secret);
    assertTrue(secret.length() > 20); // Should be long enough
  }

  @Test
  void generateApiKeyShouldReturnValidFormat() {
    String apiKey = apiKeyUtils.generateApiKey();
    assertNotNull(apiKey);
    assertTrue(apiKey.contains("."));
    String[] parts = apiKey.split("\\.");
    assertEquals(2, parts.length);
    assertTrue(parts[0].startsWith("ak_"));
    assertEquals(11, parts[0].length());
    assertTrue(parts[1].length() > 20);
  }

  @Test
  void generateApiKeyShouldGenerateDifferentApiKeys() {

    String apiKey1 = apiKeyUtils.generateApiKey();
    String apiKey2 = apiKeyUtils.generateApiKey();
    assertNotEquals(apiKey1, apiKey2);
  }

  @Test
  void extractPrefixShouldReturnPrefix() {
    String apiKey = "ak_abcdefgh.secretpart";
    String prefix = apiKeyUtils.extractPrefix(apiKey);
    assertEquals("ak_abcdefgh", prefix);
  }

  @Test
  void extractPrefixShouldThrowWhenApiKeyIsNull() {
    Exception ex =
        assertThrows(IllegalArgumentException.class, () -> apiKeyUtils.extractPrefix(null));
    assertEquals("Invalid API key format", ex.getMessage());
  }

  @Test
  void extractPrefixShouldThrowWhenApiKeyDoesNotContainDot() {
    Exception ex =
        assertThrows(IllegalArgumentException.class, () -> apiKeyUtils.extractPrefix("invalidkey"));
    assertEquals("Invalid API key format", ex.getMessage());
  }
}
