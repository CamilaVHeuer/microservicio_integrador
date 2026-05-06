package com.camicompany.microserviciointegrador.dto;

public record CreatePaymentResponseDto(
        String id_sp,
        String referencia_externa,
        String checkout_url,
        String estado_interno,
        String estado_externo
){}


