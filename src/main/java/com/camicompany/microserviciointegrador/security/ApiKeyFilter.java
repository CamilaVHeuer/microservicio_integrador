package com.camicompany.microserviciointegrador.security;

import com.camicompany.microserviciointegrador.domain.auth.ApiKey;
import com.camicompany.microserviciointegrador.repository.ApiKeyRepository;
import com.camicompany.microserviciointegrador.utils.ApiKeyUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

  private final ApiKeyUtils apiKeyUtils;
  private final ApiKeyRepository apiKeyRepository;
  private final PasswordEncoder passwordEncoder;

  public ApiKeyFilter(
      ApiKeyUtils apiKeyUtils, ApiKeyRepository apiKeyRepository, PasswordEncoder passwordEncoder) {

    this.apiKeyUtils = apiKeyUtils;
    this.apiKeyRepository = apiKeyRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String apiKey = request.getHeader("X-API-KEY");

    // 1. No viene api key
    if (apiKey == null || apiKey.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    try {

      // 2. Extraer prefix
      String prefix = apiKeyUtils.extractPrefix(apiKey);

      // 3. Buscar api key por prefix
      ApiKey storedKey = apiKeyRepository.findByKeyPrefix(prefix).orElse(null);

      // 4. No existe
      if (storedKey == null) {
        throw new BadCredentialsException("Invalid api-key");
      }

      // 5. Verificar expiración
      if (storedKey.getExpiresAt() != null
          && storedKey.getExpiresAt().isBefore(LocalDateTime.now())) {

        throw new CredentialsExpiredException("API key expired");
      }

      // 6. Verificar hash
      boolean valid = passwordEncoder.matches(apiKey, storedKey.getKeyHash());

      if (!valid) {
        throw new BadCredentialsException("Invalid api-key");
      }

      // 7. Crear autenticación
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              storedKey.getUser().getUsername(), null, Collections.emptyList());

      // 8. Guardar en contexto
      SecurityContextHolder.getContext().setAuthentication(authentication);

    } catch (IllegalArgumentException e) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key format");
      return;
    }

    // 9. Continuar
    filterChain.doFilter(request, response);
  }
}
