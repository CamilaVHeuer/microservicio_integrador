package com.camicompany.microserviciointegrador.dto.getPaymentDto;

public record HelipagosGetPaymentResponse (
        Integer id_sp,
        String estado_pago,
        String referencia_externa
){}
