package com.camicompany.microserviciointegrador.dto;

public record HelipagosGetPaymentResponse (
        Integer id_sp,
        String estado_pago,
        String referencia_externa
){}
