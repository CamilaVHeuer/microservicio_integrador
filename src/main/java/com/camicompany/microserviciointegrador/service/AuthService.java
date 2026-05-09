package com.camicompany.microserviciointegrador.service;

import com.camicompany.microserviciointegrador.dto.authDto.ApiKeyRequest;
import com.camicompany.microserviciointegrador.dto.authDto.ApiKeyResponse;
import com.camicompany.microserviciointegrador.dto.authDto.RegisterRequest;

public interface AuthService {

    ApiKeyResponse registerUser(RegisterRequest registerRequest);

    ApiKeyResponse regenerateApiKey(ApiKeyRequest apiKeyRequest);

}
