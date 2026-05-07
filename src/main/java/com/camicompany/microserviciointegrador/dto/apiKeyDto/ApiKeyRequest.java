package com.camicompany.microserviciointegrador.dto.apiKeyDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApiKeyRequest(
        @Schema(example = "John_123")
        @NotBlank(message = "The username is required")
        String username,
        @Schema(example = "J1_password")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
