package com.camicompany.microserviciointegrador.dto.createPaymentDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreatePaymentRequest (
        @Schema(example = "10000")
        @NotNull
        Long importe,
        @Schema(example = "Pago de servicios")
        @NotBlank
        String descripcion,
        @Schema(example = "2026-12-31")
        @NotNull
        @FutureOrPresent(message = "The expiration date cannot be earlier than today.")
        LocalDate fechaVto,
        @Schema(example = "REF123456")
        @NotBlank
        String referenciaExterna
){}
