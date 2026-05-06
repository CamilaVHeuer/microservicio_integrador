package com.camicompany.microserviciointegrador.mapper;

import com.camicompany.microserviciointegrador.domain.Payment;
import com.camicompany.microserviciointegrador.domain.PaymentStatus;
import com.camicompany.microserviciointegrador.dto.CreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.CreatePaymentResponseDto;
import com.camicompany.microserviciointegrador.dto.HelipagosCreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.HelipagosCreatePaymentResponse;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

public class PaymentMapper {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * 1. DTO interno → DTO Helipagos (request externo)
     */
    public static HelipagosCreatePaymentRequest toHelipagosRequest(
            CreatePaymentRequest request,
            String referenciaExterna,
            String urlRedirect,
            String webhook
    ) {

        String formattedAmount = String.format("%010d", request.importe());

        return new HelipagosCreatePaymentRequest(
                formattedAmount, // 10 digits, no decimals
                request.fechaVto().format(DATE_FORMAT), // yyyy-MM-dd
                request.descripcion(),
                referenciaExterna,
                urlRedirect,
                webhook
        );
    }

    /**
     * 2. DTO interno + response Helipagos → Entity
     */
    public static Payment toEntity(
            CreatePaymentRequest request,
            HelipagosCreatePaymentResponse response,
            String referenciaExterna
    ) {
        Payment payment = new Payment();

        payment.setReferenciaExterna(referenciaExterna);
        payment.setIdSp(String.valueOf(response.id_sp())); // lo mantenemos String
        payment.setImporte(request.importe());
        payment.setDescripcion(request.descripcion());
        payment.setFechaVto(request.fechaVto());

        payment.setCheckoutUrl(response.checkout_url());

        payment.setEstadoExterno(response.estado());
        payment.setEstadoInterno(mapEstado(response.estado()));

        return payment;
    }

    /**
     * 3. Entity → DTO salida (tu API)
     */
    public static CreatePaymentResponseDto toResponseDto(Payment payment) {
        return new CreatePaymentResponseDto(
                payment.getIdSp(),
                payment.getReferenciaExterna(),
                payment.getCheckoutUrl(),
                payment.getEstadoInterno().name(),
                payment.getEstadoExterno()
        );
    }

    /**
     * 4. Mapping estado externo → estado interno
     */
    private static PaymentStatus mapEstado(String estadoExterno) {
        if (estadoExterno == null) {
            return PaymentStatus.FAILED;
        }

        return switch (estadoExterno.toUpperCase()) {
            case "GENERADA" -> PaymentStatus.GENERATED;

            case "PROCESADA" -> PaymentStatus.PROCESSED;

            case "ACREDITADA" -> PaymentStatus.COMPLETED;

            case "RECHAZADA", "DEVUELTA", "ANULADA" -> PaymentStatus.FAILED;

            case "VENCIDA" -> PaymentStatus.CANCELLED;

            default -> PaymentStatus.FAILED;
        };
    }
}


