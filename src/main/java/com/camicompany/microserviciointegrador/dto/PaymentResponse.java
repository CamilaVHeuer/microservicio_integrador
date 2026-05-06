package com.camicompany.microserviciointegrador.dto;

public record PaymentResponse(
        Long paymentId,
        String id_sp,
        String referencia_externa,
        String estado_interno,
        String estado_externo,
        String checkout_url
){}


