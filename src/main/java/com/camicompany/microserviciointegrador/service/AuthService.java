package com.camicompany.microserviciointegrador.service;

import com.camicompany.microserviciointegrador.dto.apiKeyDto.ApiKeyRequest;
import com.camicompany.microserviciointegrador.dto.apiKeyDto.ApiKeyResponse;
import com.camicompany.microserviciointegrador.dto.registerUserDto.RegisterRequest;

public interface AuthService {

    ApiKeyResponse registerUser(RegisterRequest registerRequest);

    ApiKeyResponse regenerateApiKey(ApiKeyRequest apiKeyRequest);

}
