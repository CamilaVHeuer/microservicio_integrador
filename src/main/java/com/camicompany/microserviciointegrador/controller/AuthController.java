package com.camicompany.microserviciointegrador.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.camicompany.microserviciointegrador.dto.apiKeyDto.ApiKeyRequest;
import com.camicompany.microserviciointegrador.dto.apiKeyDto.ApiKeyResponse;
import com.camicompany.microserviciointegrador.dto.registerUserDto.RegisterRequest;
import com.camicompany.microserviciointegrador.service.AuthService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user and generate an API key")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered and API key generated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiKeyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<ApiKeyResponse> registerUser(@RequestBody @Valid RegisterRequest registerRequest) {
        ApiKeyResponse apiKeyResponse = authService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiKeyResponse);
    }

    @Operation(summary = "Regenerate an API key for an existing user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "API key regenerated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiKeyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @PostMapping("/regenerate-api-key")
    public ResponseEntity<ApiKeyResponse> regenerateApiKey(@RequestBody @Valid ApiKeyRequest apiKeyRequest) {
        ApiKeyResponse apiKeyResponse = authService.regenerateApiKey(apiKeyRequest);
        return ResponseEntity.ok(apiKeyResponse);
    }
}
