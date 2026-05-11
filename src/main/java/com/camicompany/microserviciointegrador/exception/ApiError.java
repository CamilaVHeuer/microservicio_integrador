package com.camicompany.microserviciointegrador.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Standard error response")
public record ApiError(
    @Schema(example = "400") int status,
    @Schema(example = "BAD_REQUEST") String error,
    @Schema(example = "Invalid input data") String message,
    @Schema(example = "2026-02-01T12:00:00") LocalDateTime timestamp) {}
