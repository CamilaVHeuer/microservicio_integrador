package com.camicompany.microserviciointegrador.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.camicompany.microserviciointegrador.domain.auth.ApiKey;
import com.camicompany.microserviciointegrador.domain.auth.User;
import com.camicompany.microserviciointegrador.dto.authDto.ApiKeyRequest;
import com.camicompany.microserviciointegrador.dto.authDto.ApiKeyResponse;
import com.camicompany.microserviciointegrador.dto.authDto.RegisterRequest;
import com.camicompany.microserviciointegrador.exception.InvalidCredentialsException;
import com.camicompany.microserviciointegrador.exception.StatusConflictException;
import com.camicompany.microserviciointegrador.repository.ApiKeyRepository;
import com.camicompany.microserviciointegrador.repository.UserRepository;
import com.camicompany.microserviciointegrador.utils.ApiKeyUtils;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImpTest {

  @Mock private ApiKeyRepository apiKeyRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private UserRepository userRepository;

  @Mock private ApiKeyUtils apiKeyUtils;

  @InjectMocks private AuthServiceImp authService;

  @Test
  void registerUserSuccessfully() {
    RegisterRequest req = new RegisterRequest("user1", "Password1");
    User savedUser = new User();
    savedUser.setId(1L);
    savedUser.setUsername("user1");
    savedUser.setPassword("encoded");
    when(userRepository.existsByUsername("user1")).thenReturn(false);
    when(passwordEncoder.encode("Password1")).thenReturn("encoded1");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);
    when(apiKeyUtils.generateApiKey()).thenReturn("ak_prefix.secret");
    when(apiKeyUtils.extractPrefix("ak_prefix.secret")).thenReturn("ak_prefix");
    when(passwordEncoder.encode("ak_prefix.secret")).thenReturn("hash");
    when(apiKeyRepository.save(any())).thenReturn(new ApiKey());

    ApiKeyResponse resp = authService.registerUser(req);
    assertNotNull(resp);
    assertEquals("ak_prefix.secret", resp.apiKey());
  }

  @Test
  void registerUserShouldThrowStatusConflictExceptionWhenUsernameExists() {
    RegisterRequest req = new RegisterRequest("user1", "Password1");
    when(userRepository.existsByUsername("user1")).thenReturn(true);
    assertThrows(StatusConflictException.class, () -> authService.registerUser(req));
  }

  @Test
  void regenerateApiKeySuccessfully() {
    ApiKeyRequest req = new ApiKeyRequest("user1", "Password1");
    User user = new User();
    user.setUsername("user1");
    user.setPassword("encoded1");
    user.setActive(true);
    when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("Password1", "encoded1")).thenReturn(true);
    when(apiKeyUtils.generateApiKey()).thenReturn("ak_prefix.secret");
    when(apiKeyUtils.extractPrefix("ak_prefix.secret")).thenReturn("ak_prefix");
    when(passwordEncoder.encode("ak_prefix.secret")).thenReturn("hash");
    when(apiKeyRepository.save(any())).thenReturn(new ApiKey());

    ApiKeyResponse resp = authService.regenerateApiKey(req);
    assertNotNull(resp);
    assertEquals("ak_prefix.secret", resp.apiKey());
  }

  @Test
  void regenerateApiKeyShouldThrowInvalidCredentialsExceptionWhenUserNotFound() {
    ApiKeyRequest req = new ApiKeyRequest("user1", "Password1");
    when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
    assertThrows(InvalidCredentialsException.class, () -> authService.regenerateApiKey(req));
  }

  @Test
  void regenerateApiKeyShouldThrowInvalidCredentialsExceptionWhenUserInactive() {
    ApiKeyRequest req = new ApiKeyRequest("user1", "Password1");
    User user = new User();
    user.setUsername("user1");
    user.setPassword("encoded1");
    user.setActive(false);
    when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
    assertThrows(InvalidCredentialsException.class, () -> authService.regenerateApiKey(req));
  }

  @Test
  void regenerateApiKeyShouldThrowInvalidCredentialsExceptionWhenInvalidPassword() {
    ApiKeyRequest req = new ApiKeyRequest("user1", "Password1");
    User user = new User();
    user.setUsername("user1");
    user.setPassword("encoded1");
    user.setActive(true);
    when(userRepository.findByUsername("user1")).thenReturn(java.util.Optional.of(user));
    when(passwordEncoder.matches("Password1", "encoded1")).thenReturn(false);
    assertThrows(InvalidCredentialsException.class, () -> authService.regenerateApiKey(req));
  }
}
