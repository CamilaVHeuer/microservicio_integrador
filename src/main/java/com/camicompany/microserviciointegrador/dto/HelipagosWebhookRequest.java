package com.camicompany.microserviciointegrador.dto;

public record HelipagosWebhookRequest (
        Integer id_sp,
        String estado,
        String referencia_externa,
        String medio_pago,
        String importe_abonado,
        String fecha_importe
) {
}
