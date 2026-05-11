package com.camicompany.microserviciointegrador.mapper;

import com.camicompany.microserviciointegrador.domain.payment.Payment;
import com.camicompany.microserviciointegrador.domain.payment.PaymentStatus;
import com.camicompany.microserviciointegrador.dto.*;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.CreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.HelipagosCreatePaymentRequest;
import com.camicompany.microserviciointegrador.dto.createPaymentDto.HelipagosCreatePaymentResponse;
import com.camicompany.microserviciointegrador.dto.weebhookDto.HelipagosWebhookRequest;
import java.time.format.DateTimeFormatter;

public class PaymentMapper {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

  public static HelipagosCreatePaymentRequest toHelipagosRequest(
      CreatePaymentRequest request, String urlRedirect, String webhook) {

    String formattedAmount = String.format("%010d", request.importe());

    return new HelipagosCreatePaymentRequest(
        formattedAmount, // 10 digits, no decimals
        request.fechaVto().format(DATE_FORMAT), // yyyy-MM-dd
        request.descripcion(),
        normalizeReference(request.referenciaExterna()),
        urlRedirect,
        webhook);
  }

  public static void toEntityFromHelipagosResponse(
      Payment payment, HelipagosCreatePaymentResponse response) {

    payment.setIdSp(String.valueOf(response.id_sp()));
    payment.setCheckoutUrl(response.checkout_url());
    payment.setEstadoExterno(response.estado());
    payment.setEstadoInterno(mapEstado(response.estado()));
  }

  public static void syncPaymentFromWebhook(Payment payment, HelipagosWebhookRequest request) {
    payment.setIdSp(String.valueOf(request.id_sp()));
    payment.setEstadoExterno(request.estado());
    payment.setEstadoInterno(mapEstado(request.estado()));
  }

  public static Payment toEntity(
      CreatePaymentRequest request, HelipagosCreatePaymentResponse response) {
    Payment payment = new Payment();

    payment.setReferenciaExterna(normalizeReference(request.referenciaExterna()));
    payment.setIdSp(String.valueOf(response.id_sp()));
    payment.setImporte(request.importe());
    payment.setDescripcion(request.descripcion());
    payment.setFechaVto(request.fechaVto());
    payment.setCheckoutUrl(response.checkout_url());
    payment.setEstadoExterno(response.estado());
    payment.setEstadoInterno(mapEstado(response.estado()));

    return payment;
  }

  public static PaymentResponse toResponseDto(Payment payment) {
    return new PaymentResponse(
        payment.getId(),
        payment.getIdSp(),
        payment.getReferenciaExterna(),
        payment.getEstadoInterno().name(),
        payment.getEstadoExterno(),
        payment.getCheckoutUrl());
  }

  public static void syncStatusFromHelipagos(Payment payment, String estadoExterno) {

    payment.setEstadoExterno(estadoExterno);
    payment.setEstadoInterno(mapEstado(estadoExterno));
  }

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

  public static String normalizeReference(String referencia) {

    if (referencia == null) {
      return null;
    }

    return referencia.trim().toUpperCase();
  }
}
