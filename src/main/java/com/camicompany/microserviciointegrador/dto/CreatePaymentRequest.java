package com.camicompany.microserviciointegrador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreatePaymentRequest (
        @NotNull
        Long importe,
        @NotBlank
        String descripcion,
        @NotNull
        LocalDate fechaVto,
        @NotBlank
        String referenciaExterna
){}
