package com.camicompany.microserviciointegrador.dto;

import io.swagger.v3.oas.annotations.media.Schema;


public record HelipagosWebhookRequest (
        @Schema(example="123")
        Integer id_sp,
        @Schema(example="PROCESADA")
        String estado,
        @Schema(example="REF123456")
        String referencia_externa,
        @Schema(example = "VISA")
        String medio_pago,
        @Schema(example = "10000")
        String importe_abonado,
        String fecha_importe
) {
}
