package com.camicompany.microserviciointegrador.integration;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.camicompany.microserviciointegrador.dto.authDto.ApiKeyRequest;
import com.camicompany.microserviciointegrador.dto.authDto.RegisterRequest;
import com.camicompany.microserviciointegrador.repository.ApiKeyRepository;
import com.camicompany.microserviciointegrador.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
public class AuthIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private ApiKeyRepository apiKeyRepository;

  @Autowired private ObjectMapper objectMapper;

  private static final String BASE_URL = "/api/v1/auth";

  @Test
  void registerUserShouldReturn201() throws Exception {
    RegisterRequest req = new RegisterRequest("user1", "Password1");
    mockMvc
        .perform(
            post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.apiKey").exists());

    assertTrue(userRepository.existsByUsername("user1"));
    assertEquals(1, apiKeyRepository.count());
  }

  @Test
  void registerUserWhenPasswordTooShortShouldReturn400() throws Exception {
    RegisterRequest req = new RegisterRequest("user1", "short1");
    mockMvc
        .perform(
            post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
        .andExpect(
            jsonPath("$.message").value(containsString("Password must be at least 8 characters")))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void registerUserWhenInvalidFormatPasswordShouldReturn400() throws Exception {
    RegisterRequest req = new RegisterRequest("user1", "password");
    mockMvc
        .perform(
            post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
        .andExpect(
            jsonPath("$.message")
                .value(containsString("Password must contain letters and numbers")))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void registerUserWhenUsernameIsBlankShouldReturn400() throws Exception {
    RegisterRequest req = new RegisterRequest("", "Password1");
    mockMvc
        .perform(
            post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value(containsString("The username is required")))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void registerUserWhenExistingUsernameShouldReturn409() throws Exception {
    RegisterRequest req = new RegisterRequest("user1", "Password1");

    mockMvc
        .perform(
            post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("CONFLICT"))
        .andExpect(jsonPath("$.message").value("Username already exists"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void regenerateApiKeyShouldReturn200() throws Exception {
    RegisterRequest regReq = new RegisterRequest("user2", "Password1");

    mockMvc
        .perform(
            post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regReq)))
        .andExpect(status().isCreated());

    ApiKeyRequest req = new ApiKeyRequest("user2", "Password1");
    mockMvc
        .perform(
            post(BASE_URL + "/regenerate-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apiKey").exists());
  }

  @Test
  void regenerateApiKeyWhenMissingUsernameShouldReturn400() throws Exception {
    ApiKeyRequest req = new ApiKeyRequest(null, "password1");
    mockMvc
        .perform(
            post(BASE_URL + "/regenerate-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value(containsString("The username is required")))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void regenerateApiKeyWhenInvalidFormatPasswordShouldReturn400() throws Exception {
    ApiKeyRequest req = new ApiKeyRequest("user1", "password");
    mockMvc
        .perform(
            post(BASE_URL + "/regenerate-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
        .andExpect(
            jsonPath("$.message")
                .value(containsString("Password must contain letters and numbers")))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void regenerateApiKeyWhenInvalidUsernameShouldReturn400() throws Exception {
    ApiKeyRequest req = new ApiKeyRequest("", "Password1");
    mockMvc
        .perform(
            post(BASE_URL + "/regenerate-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("The username is required")))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void regenerateApiKeyWhenInvalidCredentialsShouldReturn401() throws Exception {
    RegisterRequest regReq = new RegisterRequest("user3", "Password1");

    mockMvc
        .perform(
            post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regReq)))
        .andExpect(status().isCreated());

    ApiKeyRequest req = new ApiKeyRequest("user3", "WrongPassword1");
    mockMvc
        .perform(
            post(BASE_URL + "/regenerate-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.message").value("Invalid credentials"))
        .andExpect(jsonPath("$.timestamp").exists());
  }
}
