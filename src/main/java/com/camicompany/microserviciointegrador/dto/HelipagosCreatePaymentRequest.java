package com.camicompany.microserviciointegrador.dto;

public record HelipagosCreatePaymentRequest (
        String importe,
        String fecha_vto,
        String descripcion,
        String referencia_externa,
        String url_redirect,
        String webhook
){
}
