package com.camicompany.microserviciointegrador.dto.createPaymentDto;

public record HelipagosCreatePaymentResponse(
    Integer id_sp, String estado, String referencia_externa, String checkout_url) {}
