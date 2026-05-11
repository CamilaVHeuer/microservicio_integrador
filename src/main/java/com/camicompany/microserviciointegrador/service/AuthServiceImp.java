package com.camicompany.microserviciointegrador.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImp implements AuthService {

  private final ApiKeyRepository apiKeyRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;
  private final ApiKeyUtils apiKeyUtils;

  public AuthServiceImp(
      ApiKeyRepository apiKeyRepository,
      PasswordEncoder passwordEncoder,
      UserRepository userRepository,
      ApiKeyUtils apiKeyUtils) {
    this.apiKeyRepository = apiKeyRepository;
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
    this.apiKeyUtils = apiKeyUtils;
  }

  @Override
  public ApiKeyResponse registerUser(RegisterRequest registerRequest) {
    String username = registerRequest.username();
    String password = registerRequest.password();

    if (userRepository.existsByUsername(username)) {
      throw new StatusConflictException("Username already exists");
    }
    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    User savedUser = userRepository.save(user);

    return createAndSaveApiKey(savedUser);
  }

  @Override
  public ApiKeyResponse regenerateApiKey(ApiKeyRequest apiKeyRequest) {
    String username = apiKeyRequest.username();
    String password = apiKeyRequest.password();

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

    if (!user.getActive()) {
      throw new InvalidCredentialsException("User inactive");
    }

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new InvalidCredentialsException("Invalid credentials");
    }

    return createAndSaveApiKey(user);
  }

  // helper method
  private ApiKeyResponse createAndSaveApiKey(User user) {
    String apiKeyGenerated = apiKeyUtils.generateApiKey();
    String prefix = apiKeyUtils.extractPrefix(apiKeyGenerated);
    String keyHash = passwordEncoder.encode(apiKeyGenerated);

    ApiKey apikey = new ApiKey();
    apikey.setKeyPrefix(prefix);
    apikey.setKeyHash(keyHash);
    apikey.setUser(user);
    apikey.setExpiresAt(null);

    apiKeyRepository.save(apikey);

    return new ApiKeyResponse(apiKeyGenerated);
  }
}
