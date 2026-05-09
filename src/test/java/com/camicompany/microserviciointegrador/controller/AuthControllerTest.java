package com.camicompany.microserviciointegrador.controller;

import com.camicompany.microserviciointegrador.security.ApiKeyFilter;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import com.camicompany.microserviciointegrador.dto.authDto.ApiKeyRequest;
import com.camicompany.microserviciointegrador.dto.authDto.ApiKeyResponse;
import com.camicompany.microserviciointegrador.dto.authDto.RegisterRequest;
import com.camicompany.microserviciointegrador.exception.StatusConflictException;
import com.camicompany.microserviciointegrador.exception.InvalidCredentialsException;
import com.camicompany.microserviciointegrador.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration =  SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthService authService;

    @MockitoBean
    private ApiKeyFilter apiKeyFilter;

	private static final String BASE_URL = "/api/v1/auth";

	@Test
	void registerUserShouldReturn201() throws Exception {
		RegisterRequest req = new RegisterRequest("user1", "Password1");
		ApiKeyResponse resp = new ApiKeyResponse("ak_api.Key");
		when(authService.registerUser(any(RegisterRequest.class))).thenReturn(resp);
		mockMvc.perform(post(BASE_URL + "/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.apiKey").value("ak_api.Key"));

        verify(authService).registerUser(any(RegisterRequest.class));
	}

	@Test
	void registerUserWhehPasswordTooShortShouldReturn400() throws Exception {
		RegisterRequest req = new RegisterRequest("user1", "short1");
		mockMvc.perform(post(BASE_URL + "/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("Password must be at least 8 characters")))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(authService);
	}

	@Test
	void registerUserWhenInvalidFormatPasswordShouldReturn400() throws Exception {
		RegisterRequest req = new RegisterRequest("user1", "password");
		mockMvc.perform(post(BASE_URL + "/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("Password must contain letters and numbers")))
                .andExpect(jsonPath("$.timestamp").exists());
        verifyNoInteractions(authService);
	}

	@Test
	void registerUserWhenUsernameIsBlankShouldReturn400() throws Exception {
		RegisterRequest req = new RegisterRequest("", "Password1");
		mockMvc.perform(post(BASE_URL + "/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("The username is required")))
                .andExpect(jsonPath("$.timestamp").exists());
        verifyNoInteractions(authService);
	}

	@Test
	void registerUserWhenExistingUsernameShouldReturn409() throws Exception {
		RegisterRequest req = new RegisterRequest("user1", "Password1");
		when(authService.registerUser(any())).thenThrow(new StatusConflictException("Username already exists"));
		mockMvc.perform(post(BASE_URL + "/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Username already exists"))
                .andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void regenerateApiKeyShouldReturn200() throws Exception {
		ApiKeyRequest req = new ApiKeyRequest("user1", "Password1");
		ApiKeyResponse resp = new ApiKeyResponse("ak_api.Key");
		when(authService.regenerateApiKey(any())).thenReturn(resp);
		mockMvc.perform(post(BASE_URL + "/regenerate-api-key")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.apiKey").value("ak_api.Key"));

        verify(authService).regenerateApiKey(any(ApiKeyRequest.class));
	}

	@Test
	void regenerateApiKeyWhenMissingUsernameShouldReturn400() throws Exception {
		ApiKeyRequest req = new ApiKeyRequest(null, "password1");
		mockMvc.perform(post(BASE_URL + "/regenerate-api-key")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("The username is required")))
                .andExpect(jsonPath("$.timestamp").exists());
        verifyNoInteractions(authService);
	}

    @Test
    void regenerateApiKeyWhenMissingPasswordShouldReturn400() throws Exception {
        ApiKeyRequest req = new ApiKeyRequest("user1", null);
        mockMvc.perform(post(BASE_URL + "/regenerate-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("The password is required")))
                .andExpect(jsonPath("$.timestamp").exists());
        verifyNoInteractions(authService);
    }

	@Test
	void regenerateApiKeyWhenPasswordTooShortShouldReturn400() throws Exception {
		ApiKeyRequest req = new ApiKeyRequest("user1", "short1");
		mockMvc.perform(post(BASE_URL + "/regenerate-api-key")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("Password must be at least 8 characters")))
                .andExpect(jsonPath("$.timestamp").exists());
        verifyNoInteractions(authService);
	}

    @Test
    void regenerateApiKeyWhenInvalidFormatPasswordShouldReturn400() throws Exception {
        ApiKeyRequest req = new ApiKeyRequest("user1", "password");
        mockMvc.perform(post(BASE_URL + "/regenerate-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("Password must contain letters and numbers")))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(authService);
    }

	@Test
	void regenerateApiKeyWhenInvalidUsernameShouldReturn400() throws Exception {
		ApiKeyRequest req = new ApiKeyRequest("", "Password1");
		mockMvc.perform(post(BASE_URL + "/regenerate-api-key")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("The username is required")))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(authService);

	}

	@Test
	void regenerateApiKeyWhenInvalidCredentialsShouldReturn401() throws Exception {
		ApiKeyRequest req = new ApiKeyRequest("user1", "Password1");
		when(authService.regenerateApiKey(any())).thenThrow(new InvalidCredentialsException("Invalid credentials"));
		mockMvc.perform(post(BASE_URL + "/regenerate-api-key")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.timestamp").exists());
	}
}


